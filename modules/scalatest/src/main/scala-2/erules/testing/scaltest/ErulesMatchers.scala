package erules.testing.scaltest

import erules.core.{EvalRuleResult, EvalRulesInterpreterResult, Rule}
import org.scalatest.matchers.{BeMatcher, MatchResult, Matcher}

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

trait ErulesMatchers extends ErulesEvalRulesInterpreterResultMatchers with ErulesEvalRuleResultMatchers

sealed trait ErulesRuleTypedEvaluatedMatchers {

  def executedInMax[T](maxDuration: FiniteDuration): Matcher[Rule.Evaluated[T]] =
    (actual: Rule.Evaluated[T]) => {

      val actualET = actual.executionTime.getOrElse(FiniteDuration(0, MILLISECONDS)).toMillis
      val expectedET = maxDuration.toMillis

      MatchResult(
        matches = actualET <= expectedET,
        rawFailureMessage = s"Rule execution time should be <= then ${expectedET}ms but got $actual",
        rawNegatedFailureMessage = s"Rule execution time should be > then ${expectedET}ms but got $actual"
      )
    }
}

sealed trait ErulesEvalRulesInterpreterResultMatchers {

  case object Allowed extends BeMatcher[EvalRulesInterpreterResult[Any]] {
    override def apply(left: EvalRulesInterpreterResult[Any]): MatchResult =
      MatchResult(
        matches = left.isAllowed,
        rawFailureMessage = s"Expected to be Allowed but got ${left.typeName}",
        rawNegatedFailureMessage = s"Expected to be Denied but got ${left.typeName}"
      )
  }

  case object Denied extends BeMatcher[EvalRulesInterpreterResult[Any]] {
    override def apply(left: EvalRulesInterpreterResult[Any]): MatchResult =
      MatchResult(
        matches = left.isDenied,
        rawFailureMessage = s"Expected to be Denied but got ${left.typeName}",
        rawNegatedFailureMessage = s"Expected to be Allowed but got ${left.typeName}"
      )
  }
}

sealed trait ErulesEvalRuleResultMatchers {

  case object Allow extends BeMatcher[EvalRuleResult] {
    override def apply(left: EvalRuleResult): MatchResult =
      MatchResult(
        matches = left.isAllow,
        rawFailureMessage = s"Expected to be Allow but got ${left.typeName}",
        rawNegatedFailureMessage = s"Expected to be NOT Allow but got ${left.typeName}"
      )
  }

  case object Deny extends BeMatcher[EvalRuleResult] {
    override def apply(left: EvalRuleResult): MatchResult =
      MatchResult(
        matches = left.isDeny,
        rawFailureMessage = s"Expected to be Deny but got ${left.typeName}",
        rawNegatedFailureMessage = s"Expected to be NOT Deny but got ${left.typeName}"
      )
  }

  case object Ignore extends BeMatcher[EvalRuleResult] {
    override def apply(left: EvalRuleResult): MatchResult =
      MatchResult(
        matches = left.isIgnore,
        rawFailureMessage = s"Expected to be Ignore but got ${left.typeName}",
        rawNegatedFailureMessage = s"Expected to be NOT Ignore but got ${left.typeName}"
      )
  }
}
