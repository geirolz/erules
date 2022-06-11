package erules.core

import cats.data.NonEmptyList
import erules.core.RuleVerdict.{Allow, Deny, Ignore}

import scala.util.{Failure, Success}

trait RuleResultsInterpreter {
  def interpret[T](report: NonEmptyList[RuleResult.Free[T]]): RuleResultsInterpreterVerdict[T]
}
object RuleResultsInterpreter extends EvalResultsInterpreterInstances {
  object Defaults {
    val allowAllNotDenied: RuleResultsInterpreter = new AllowAllNotDeniedRuleResultsInterpreter
    val denyAllNotAllowed: RuleResultsInterpreter = new DenyAllNotAllowedRuleResultsInterpreter
  }
}

private[erules] trait EvalResultsInterpreterInstances {

  class AllowAllNotDeniedRuleResultsInterpreter extends RuleResultsInterpreter {
    override def interpret[T](
      report: NonEmptyList[RuleResult.Free[T]]
    ): RuleResultsInterpreterVerdict[T] =
      partialEval(report).getOrElse(
        RuleResultsInterpreterVerdict.Allowed(
          NonEmptyList.one(
            RuleResult.noMatch[T, Allow](Allow.allNotExplicitlyDenied)
          )
        )
      )
  }

  class DenyAllNotAllowedRuleResultsInterpreter extends RuleResultsInterpreter {
    override def interpret[T](
      report: NonEmptyList[RuleResult.Free[T]]
    ): RuleResultsInterpreterVerdict[T] =
      partialEval(report).getOrElse(
        RuleResultsInterpreterVerdict.Denied(
          NonEmptyList.one(
            RuleResult.noMatch[T, Deny](Deny.allNotExplicitlyAllowed)
          )
        )
      )
  }

  private def partialEval[T](
    report: NonEmptyList[RuleResult.Free[T]]
  ): Option[RuleResultsInterpreterVerdict[T]] = {
    type Res[+V <: RuleVerdict] = RuleResult[T, V]

    report.toList
      .flatMap {
        case _ @RuleResult(_: Rule[?, T], Success(_: Ignore), _) =>
          None
        case _ @RuleResult(r: Rule[?, T], Failure(ex), _) =>
          Some(Left(RuleResult.denyForSafetyInCaseOfError(r.asInstanceOf[Rule[Nothing, T]], ex)))
        case re @ RuleResult(_: Rule[?, T], Success(_: Deny), _) =>
          Some(Left(re.asInstanceOf[Res[Deny]]))
        case re @ RuleResult(_: Rule[?, T], Success(_: Allow), _) =>
          Some(Right(re.asInstanceOf[Res[Allow]]))
      }
      .partitionMap[Res[Deny], Res[Allow]](identity) match {
      case (_ @Nil, _ @Nil) =>
        None
      case (_ @Nil, allow :: allows) =>
        Some(RuleResultsInterpreterVerdict.Allowed(NonEmptyList.of(allow, allows*)))
      case (deny :: denials, _) =>
        Some(RuleResultsInterpreterVerdict.Denied(NonEmptyList.of(deny, denials*)))
    }
  }
}
