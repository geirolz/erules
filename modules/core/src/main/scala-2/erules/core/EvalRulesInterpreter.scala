package erules.core

import cats.data.NonEmptyList
import erules.core.EvalRuleResult.{Allow, Deny, Ignore}
import erules.core.Rule.Evaluated

import scala.util.{Failure, Success}

trait EvalRulesInterpreter {
  def interpret[T](report: NonEmptyList[Rule.Evaluated[T]]): EvalRulesInterpreterResult[T]
}
object EvalRulesInterpreter extends EvalResultsInterpreterInstances {
  object Defaults {
    val allowAllNotDenied: EvalRulesInterpreter = new AllowAllNotDeniedEvalRulesInterpreter()
    val denyAllNotAllowed: EvalRulesInterpreter = new DenyAllNotAllowedEvalRulesInterpreter()
  }
}

private[erules] trait EvalResultsInterpreterInstances {

  class AllowAllNotDeniedEvalRulesInterpreter extends EvalRulesInterpreter {
    override def interpret[T](report: NonEmptyList[Evaluated[T]]): EvalRulesInterpreterResult[T] =
      partialEval(report).getOrElse(
        EvalRulesInterpreterResult.Allowed(
          NonEmptyList.one(
            Rule.TypedEvaluated.noMatch(Allow.allNotExplicitlyDenied)
          )
        )
      )
  }

  class DenyAllNotAllowedEvalRulesInterpreter extends EvalRulesInterpreter {
    override def interpret[T](report: NonEmptyList[Evaluated[T]]): EvalRulesInterpreterResult[T] =
      partialEval(report).getOrElse(
        EvalRulesInterpreterResult.Denied(
          NonEmptyList.one(
            Rule.TypedEvaluated.noMatch(Deny.allNotExplicitlyAllowed)
          )
        )
      )
  }

  private def partialEval[T](report: NonEmptyList[Rule.Evaluated[T]]): Option[EvalRulesInterpreterResult[T]] = {
    type Eval[+R <: EvalRuleResult] = Rule.TypedEvaluated[T, R]

    report.toList
      .flatMap {
        case _ @Rule.TypedEvaluated(_: Rule[T], Success(_: Ignore), _) =>
          None
        case _ @Rule.TypedEvaluated(r: Rule[T], Failure(ex), _) =>
          Some(Left(Rule.TypedEvaluated.denyForSafetyInCaseOfError(r, ex)))
        case re @ Rule.TypedEvaluated(_: Rule[T], Success(_: Deny), _) =>
          Some(Left(re.asInstanceOf[Eval[Deny]]))
        case re @ Rule.TypedEvaluated(_: Rule[T], Success(_: Allow), _) =>
          Some(Right(re.asInstanceOf[Eval[Allow]]))
      }
      .partitionMap[Eval[Deny], Eval[Allow]](identity) match {
      case (_ @Nil, _ @Nil) =>
        None
      case (_ @Nil, allow :: allows) =>
        Some(EvalRulesInterpreterResult.Allowed(NonEmptyList.of(allow, allows*)))
      case (deny :: denials, _) =>
        Some(EvalRulesInterpreterResult.Denied(NonEmptyList.of(deny, denials*)))
    }
  }
}
