package erules

import cats.{Applicative, ApplicativeThrow, Id, Parallel}
import cats.data.NonEmptyList
import cats.effect.kernel.Async
import cats.effect.Sync
import erules.utils.IsId

case class RulesEngine[F[_], T] private (
  rules: NonEmptyList[Rule[F, T]],
  interpreter: RuleResultsInterpreter
) {

  import cats.implicits.*
  import erules.utils.IsId.*

  // execution
  def parEval(data: T)(implicit
    F: Async[F],
    P: Parallel[F]
  ): F[EngineResult[T]] =
    createResult(
      data,
      rules.parTraverse(_.eval(data))
    )

  def parEvalN(data: T, parallelismLevel: Int)(implicit F: Async[F]): F[EngineResult[T]] =
    createResult(
      data,
      F.parTraverseN(parallelismLevel)(rules)(_.eval(data))
    )

  def seqEval(data: T)(implicit F: Sync[F]): F[EngineResult[T]] =
    createResult(
      data,
      rules.map(_.eval(data)).sequence
    )

  def pureSeqEval(data: T)(implicit F: IsId[F]): EngineResult[T] =
    createResult(
      data,
      rules.map(_.pureEval(data)).liftId[F]
    )(F.applicative)

  private def createResult(
    data: T,
    evalRes: F[NonEmptyList[RuleResult.Unbiased]]
  )(implicit F: Applicative[F]): F[EngineResult[T]] =
    evalRes
      .map(evaluatedRules =>
        EngineResult(
          data    = data,
          verdict = interpreter.interpret(evaluatedRules)
        )
      )
}
object RulesEngine {

  def apply[F[_]: Applicative]: RulesEngineRulesBuilder[F] =
    new RulesEngineRulesBuilder[F]

  def pure: RulesEngineRulesBuilder[Id] =
    apply[Id]

  class RulesEngineRulesBuilder[F[_]: Applicative] private[RulesEngine] {

    // effect
    def withRules[T](
      head1: Rule[F, T],
      tail: Rule[F, T]*
    ): RulesEngineIntBuilder[F, T] =
      withRules[T](NonEmptyList.of[Rule[F, T]](head1, tail*))

    def withRules[T](rules: NonEmptyList[Rule[F, T]]): RulesEngineIntBuilder[F, T] =
      new RulesEngineIntBuilder[F, T](rules)

    // pure
    def withRules[G[_]: IsId, T](
      head1: Rule[G, T],
      tail: Rule[G, T]*
    ): RulesEngineIntBuilder[F, T] =
      withRules[T](NonEmptyList.of[Rule[G, T]](head1, tail*).mapLift[F])

    def withRules[G[_]: IsId, T](
      rules: NonEmptyList[Rule[G, T]]
    ): RulesEngineIntBuilder[F, T] =
      withRules[T](rules.mapLift[F])
  }

  class RulesEngineIntBuilder[F[_]: Applicative, T] private[RulesEngine] (
    rules: NonEmptyList[Rule[F, T]]
  ) {

    def withInterpreter[G[_]: ApplicativeThrow](
      interpreter: RuleResultsInterpreter
    ): G[RulesEngine[F, T]] =
      Rule.findDuplicated(rules) match {
        case Nil =>
          ApplicativeThrow[G].pure(RulesEngine(rules, interpreter))
        case duplicatedDescriptions =>
          ApplicativeThrow[G].raiseError(DuplicatedRulesException(duplicatedDescriptions))
      }

    def allowAllNotDenied[G[_]: ApplicativeThrow]: G[RulesEngine[F, T]] =
      withInterpreter(RuleResultsInterpreter.Defaults.allowAllNotDenied)

    def denyAllNotAllowed[G[_]: ApplicativeThrow]: G[RulesEngine[F, T]] =
      withInterpreter(RuleResultsInterpreter.Defaults.denyAllNotAllowed)
  }

  case class DuplicatedRulesException[F[_], T](duplicates: List[Rule[F, T]])
      extends RuntimeException(s"Duplicated rules found!\n${duplicates
          .map(_.fullDescription.prependedAll("- ").mkString)
          .mkString(",")}")
}
