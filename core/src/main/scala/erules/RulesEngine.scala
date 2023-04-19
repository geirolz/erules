package erules

import cats.{~>, Applicative, ApplicativeThrow, Parallel}
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

  def seqEvalPure(data: T)(implicit F: IsId[F]): EngineResult[T] =
    createResult(
      data,
      rules.map(_.evalPure(data)).liftId[F]
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

  def withRules[F[_], T](head1: Rule[F, T], tail: Rule[F, T]*): RulesEngineIntBuilder[F, T] =
    withRules[F, T](NonEmptyList.of[Rule[F, T]](head1, tail*))

  def withRules[F[_], T](rules: NonEmptyList[Rule[F, T]]): RulesEngineIntBuilder[F, T] =
    new RulesEngineIntBuilder[F, T](rules)

  class RulesEngineIntBuilder[F[_], T] private[RulesEngine] (
    rules: NonEmptyList[Rule[F, T]]
  ) {

    def withInterpreter[G[_]: ApplicativeThrow](
      interpreter: RuleResultsInterpreter
    ): G[RulesEngine[F, T]] =
      Rule.findDuplicated(rules) match {
        case Nil =>
          ApplicativeThrow[G].pure(RulesEngine(rules, interpreter))
        case duplicatedDescriptions =>
          ApplicativeThrow[G].raiseError(DuplicatedRulesError(duplicatedDescriptions))
      }

    def allowAllNotDenied[G[_]: ApplicativeThrow]: G[RulesEngine[F, T]] =
      withInterpreter(RuleResultsInterpreter.Defaults.allowAllNotDenied)

    def denyAllNotAllowed[G[_]: ApplicativeThrow]: G[RulesEngine[F, T]] =
      withInterpreter(RuleResultsInterpreter.Defaults.denyAllNotAllowed)

    def liftK[G[_]: Applicative](implicit isId: IsId[F]): RulesEngineIntBuilder[G, T] =
      mapK[G](new ~>[F, G] {
        def apply[A](fa: F[A]): G[A] = Applicative[G].pure(isId.unliftId(fa))
      })

    def mapK[G[_]](fg: F ~> G): RulesEngineIntBuilder[G, T] =
      new RulesEngineIntBuilder[G, T](rules.map(_.mapK(fg)))
  }

  case class DuplicatedRulesError[F[_], T](duplicates: List[Rule[F, T]])
      extends RuntimeException(s"Duplicated rules found!\n${duplicates
          .map(_.fullDescription.prependedAll("- ").mkString)
          .mkString(",")}")
}
