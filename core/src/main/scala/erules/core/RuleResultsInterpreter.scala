package erules.core

import cats.data.NonEmptyList
import erules.core.RuleVerdict.{Allow, Deny, Ignore}

trait RuleResultsInterpreter {
  def interpret(report: NonEmptyList[RuleResult.Unbiased]): RuleResultsInterpreterVerdict
}
object RuleResultsInterpreter extends EvalResultsInterpreterInstances {
  object Defaults {
    val allowAllNotDenied: RuleResultsInterpreter = new AllowAllNotDeniedRuleResultsInterpreter
    val denyAllNotAllowed: RuleResultsInterpreter = new DenyAllNotAllowedRuleResultsInterpreter
  }
}

private[erules] trait EvalResultsInterpreterInstances {

  class AllowAllNotDeniedRuleResultsInterpreter extends RuleResultsInterpreter {
    override def interpret(
      report: NonEmptyList[RuleResult.Unbiased]
    ): RuleResultsInterpreterVerdict =
      partialEval(report).getOrElse(
        RuleResultsInterpreterVerdict.Allowed(
          NonEmptyList.one(
            RuleResult.noMatch[Allow](Allow.allNotExplicitlyDenied)
          )
        )
      )
  }

  class DenyAllNotAllowedRuleResultsInterpreter extends RuleResultsInterpreter {
    override def interpret(
      report: NonEmptyList[RuleResult.Unbiased]
    ): RuleResultsInterpreterVerdict =
      partialEval(report).getOrElse(
        RuleResultsInterpreterVerdict.Denied(
          NonEmptyList.one(
            RuleResult.noMatch[Deny](Deny.allNotExplicitlyAllowed)
          )
        )
      )
  }

  private def partialEval(
    report: NonEmptyList[RuleResult.Unbiased]
  ): Option[RuleResultsInterpreterVerdict] = {
    type Res[+V <: RuleVerdict] = RuleResult[V]

    report.toList
      .flatMap {
        case _ @RuleResult(_: RuleInfo, Right(_: Ignore), _) =>
          None
        case _ @RuleResult(info: RuleInfo, Left(ex), _) =>
          Some(
            Left(RuleResult(info).denyForSafetyInCaseOfError(ex).asInstanceOf[Res[Deny]])
          )
        case re @ RuleResult(_: RuleInfo, Right(_: Deny), _) =>
          Some(Left(re.asInstanceOf[Res[Deny]]))
        case re @ RuleResult(_: RuleInfo, Right(_: Allow), _) =>
          Some(Right(re.asInstanceOf[Res[Allow]]))
      }
      .partitionMap[Res[Deny], Res[Allow]]((a: Either[Res[Deny], Res[Allow]]) => a) match {
      case (_ @Nil, _ @Nil) =>
        None
      case (_ @Nil, allow :: allows) =>
        Some(RuleResultsInterpreterVerdict.Allowed(NonEmptyList.of(allow, allows*)))
      case (deny :: denials, _) =>
        Some(RuleResultsInterpreterVerdict.Denied(NonEmptyList.of(deny, denials*)))
    }
  }
}
