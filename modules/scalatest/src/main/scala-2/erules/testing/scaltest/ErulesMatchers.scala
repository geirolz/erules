package erules.testing.scaltest

import erules.core.{RuleResult, RuleResultsInterpreterVerdict, RuleVerdict}
import org.scalatest.matchers.{BeMatcher, MatchResult, Matcher}

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

trait ErulesMatchers extends ErulesRuleResultsInterpreterVerdictMatchers with ErulesRuleVerdictMatchers

trait ErulesRuleTypedEvaluatedMatchers {

  def executedInMax[T](maxDuration: FiniteDuration): Matcher[RuleResult.Free[T]] =
    (actual: RuleResult.Free[T]) => {

      val actualET = actual.executionTime.getOrElse(FiniteDuration(0, MILLISECONDS)).toMillis
      val expectedET = maxDuration.toMillis

      MatchResult(
        matches = actualET <= expectedET,
        rawFailureMessage = s"Rule execution time should be <= then ${expectedET}ms but got $actual",
        rawNegatedFailureMessage = s"Rule execution time should be > then ${expectedET}ms but got $actual"
      )
    }
}

trait ErulesRuleResultsInterpreterVerdictMatchers {

  val allowed: BeMatcher[RuleResultsInterpreterVerdict[Nothing]] =
    (left: RuleResultsInterpreterVerdict[Nothing]) =>
      MatchResult(
        matches = left.isAllowed,
        rawFailureMessage = s"Expected to be Allowed but got ${left.typeName}",
        rawNegatedFailureMessage = s"Expected to be Denied but got ${left.typeName}"
      )

  val denied: BeMatcher[RuleResultsInterpreterVerdict[Nothing]] =
    (left: RuleResultsInterpreterVerdict[Nothing]) =>
      MatchResult(
        matches = left.isDenied,
        rawFailureMessage = s"Expected to be Denied but got ${left.typeName}",
        rawNegatedFailureMessage = s"Expected to be Allowed but got ${left.typeName}"
      )
}

trait ErulesRuleVerdictMatchers {

  val allow: BeMatcher[RuleVerdict] = (left: RuleVerdict) =>
    MatchResult(
      matches = left.isAllow,
      rawFailureMessage = s"Expected to be Allow but got ${left.typeName}",
      rawNegatedFailureMessage = s"Expected to be NOT Allow but got ${left.typeName}"
    )

  val deny: BeMatcher[RuleVerdict] = (left: RuleVerdict) =>
    MatchResult(
      matches = left.isDeny,
      rawFailureMessage = s"Expected to be Deny but got ${left.typeName}",
      rawNegatedFailureMessage = s"Expected to be NOT Deny but got ${left.typeName}"
    )

  val ignore: BeMatcher[RuleVerdict] = (left: RuleVerdict) =>
    MatchResult(
      matches = left.isIgnore,
      rawFailureMessage = s"Expected to be Ignore but got ${left.typeName}",
      rawNegatedFailureMessage = s"Expected to be NOT Ignore but got ${left.typeName}"
    )
}
