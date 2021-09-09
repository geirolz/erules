package erules.core

import cats.effect.{IO, LiftIO}
import cats.MonadThrow
import cats.data.NonEmptyList
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.util.{Failure, Success}

class RulesEngine[T] private (val rules: NonEmptyList[Rule[T]], interpreter: RuleResultsInterpreter) {

  import cats.implicits.*

  private implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLoggerFromName[IO]("RuleEngine")

  def parEval[F[_]: LiftIO](data: T): F[EngineResult[T]] =
    createResult[F](
      data,
      rules.parTraverse(evalZipRuleLogging(data, _))
    )

  def parEvalN[F[_]: LiftIO](data: T, parallelismLevel: Int): F[EngineResult[T]] =
    createResult[F](
      data,
      IO.parTraverseN(parallelismLevel)(rules)(evalZipRuleLogging(data, _))
    )

  def seqEval[F[_]: LiftIO](data: T): F[EngineResult[T]] =
    createResult[F](
      data,
      rules.map(evalZipRuleLogging(data, _)).sequence
    )

  private def createResult[F[_]: LiftIO](
    data: T,
    evalRes: IO[NonEmptyList[RuleResult.Free[T]]]
  ): F[EngineResult[T]] =
    evalRes
      .map(evaluatedRules =>
        EngineResult(
          data = data,
          verdict = interpreter.interpret(evaluatedRules)
        )
      )
      .to[F]

  private def evalZipRuleLogging(data: T, rule: Rule[T]): IO[RuleResult.Free[T]] =
    rule
      .eval(data)
      .flatTap {
        case RuleResult(_, Success(_), _)     => IO.unit
        case RuleResult(rule, Failure(ex), _) => logger.info(ex)(s"$rule failed!")
      }
}
object RulesEngine {

  def apply[F[_]: MonadThrow, T](rules: NonEmptyList[Rule[T]])(interpreter: RuleResultsInterpreter): F[RulesEngine[T]] =
    Rule.findDuplicated(rules) match {
      case Nil                    => MonadThrow[F].pure(new RulesEngine(rules, interpreter))
      case duplicatedDescriptions => MonadThrow[F].raiseError(DuplicatedRulesException(duplicatedDescriptions))
    }

  def allowAllNotDenied[F[_]: MonadThrow, T](rules: NonEmptyList[Rule[T]]): F[RulesEngine[T]] =
    RulesEngine[F, T](rules)(RuleResultsInterpreter.Defaults.allowAllNotDenied)

  def denyAllNotAllowed[F[_]: MonadThrow, T](rules: NonEmptyList[Rule[T]]): F[RulesEngine[T]] =
    RulesEngine[F, T](rules)(RuleResultsInterpreter.Defaults.denyAllNotAllowed)

  case class DuplicatedRulesException(duplicates: List[Rule[?]])
      extends RuntimeException(s"Duplicated rules found!\n${duplicates
        .map(_.fullDescription.prepended("- ").mkString)
        .mkString(",")}")
}
