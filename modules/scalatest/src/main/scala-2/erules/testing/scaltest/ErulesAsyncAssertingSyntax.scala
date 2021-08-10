package erules.testing.scaltest

import erules.core.{EvalRuleResult, Rule}
import org.scalatest.matchers.should.Matchers
import org.scalatest.Assertion

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

trait ErulesAsyncAssertingSyntax { this: Matchers =>

  implicit class RuleTypedEvaluatedAssertingOps[-T, +R <: EvalRuleResult](evaluatedRule: Rule.TypedEvaluated[T, R]) {

    def executedIn(maxDuration: FiniteDuration): Assertion =
      evaluatedRule.executionTime.getOrElse(FiniteDuration(0, MILLISECONDS)).toMillis should be <= maxDuration.toMillis

  }
}
