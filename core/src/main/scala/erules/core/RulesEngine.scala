package erules.core

import cats.{Functor, Id, MonadThrow, Parallel}
import cats.data.NonEmptyList
import cats.effect.kernel.Async
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.annotation.unused

case class RulesEngine[F[_], T] private (
  rules: NonEmptyList[Rule[F, T]],
  private val interpreter: RuleResultsInterpreter,
  private val logger: Option[StructuredLogger[F]] = None
) {

  import cats.implicits.*

  // logger
  def withLogger(_logger: StructuredLogger[F]): RulesEngine[F, T] =
    copy(logger = Some(_logger))

  def withLogger(implicit @unused F: Async[F]): RulesEngine[F, T] =
    copy(logger = Some(Slf4jLogger.getLogger[F]))

  // execution
  def parEval(data: T)(implicit
    F: Async[F],
    P: Parallel[F]
  ): F[EngineResult[T]] =
    createResult(
      data,
      rules.parTraverse(evalZipRuleLogging(data, _))
    )

  def parEvalN(data: T, parallelismLevel: Int)(implicit F: Async[F]): F[EngineResult[T]] =
    createResult(
      data,
      F.parTraverseN(parallelismLevel)(rules)(evalZipRuleLogging(data, _))
    )

  def seqEval(data: T)(implicit F: Async[F]): F[EngineResult[T]] =
    createResult(
      data,
      rules.map(evalZipRuleLogging(data, _)).sequence
    )

  private def createResult(
    data: T,
    evalRes: F[NonEmptyList[RuleResult.Free[T]]]
  )(implicit F: Functor[F]): F[EngineResult[T]] =
    evalRes
      .map(evaluatedRules =>
        EngineResult(
          data    = data,
          verdict = interpreter.interpret(evaluatedRules)
        )
      )

  private def evalZipRuleLogging(data: T, rule: Rule[F, T])(implicit
    F: Async[F]
  ): F[RuleResult.Free[T]] = {
    rule
      .eval(data)
      .flatTap {
        case RuleResult(_, Right(_), _) => F.unit
        case RuleResult(rule, Left(ex), _) =>
          logger match {
            case Some(l) => l.info(ex)(s"$rule failed!")
            case None    => F.unit
          }
      }
  }
}
object RulesEngine {

  def apply[F[_]: MonadThrow]: RulesEngineRulesBuilder[F] =
    new RulesEngineRulesBuilder[F]

  class RulesEngineRulesBuilder[F[_]: MonadThrow] private[RulesEngine] {

    // effect
    def withRules[T](
      head1: Rule[F, T],
      tail: Rule[F, T]*
    ): RulesEngineIntBuilder[F, T] =
      withRules[T](NonEmptyList.of[Rule[F, T]](head1, tail*))

    def withRules[T](rules: NonEmptyList[Rule[F, T]]): RulesEngineIntBuilder[F, T] =
      new RulesEngineIntBuilder[F, T](rules)

    // pure
    def withRules[G[X] <: Id[X], T](
      head1: Rule[G, T],
      tail: Rule[G, T]*
    )(implicit env: G[Any] <:< Id[Any]): RulesEngineIntBuilder[F, T] =
      withRules[T](NonEmptyList.of[Rule[Id, T]](head1, tail*).mapLift[F])

    def withRules[G[X] <: Id[X], T](rules: NonEmptyList[PureRule[T]])(implicit
      env: G[Any] <:< Id[Any]
    ): RulesEngineIntBuilder[F, T] =
      withRules[T](rules.mapLift[F])
  }

  class RulesEngineIntBuilder[F[_]: MonadThrow, T] private[RulesEngine] (
    rules: NonEmptyList[Rule[F, T]]
  ) {

    def withInterpreter(interpreter: RuleResultsInterpreter): F[RulesEngine[F, T]] =
      Rule.findDuplicated(rules) match {
        case Nil =>
          MonadThrow[F].pure(RulesEngine(rules, interpreter))
        case duplicatedDescriptions =>
          MonadThrow[F].raiseError(DuplicatedRulesException(duplicatedDescriptions))
      }

    def allowAllNotDenied: F[RulesEngine[F, T]] =
      withInterpreter(RuleResultsInterpreter.Defaults.allowAllNotDenied)

    def denyAllNotAllowed: F[RulesEngine[F, T]] =
      withInterpreter(RuleResultsInterpreter.Defaults.denyAllNotAllowed)
  }

  case class DuplicatedRulesException(duplicates: List[AnyRule])
      extends RuntimeException(s"Duplicated rules found!\n${duplicates
          .map(_.fullDescription.prepended("- ").mkString)
          .mkString(",")}")
}
