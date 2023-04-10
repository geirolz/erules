package erules

import cats.data.NonEmptyList
import erules.RuleResultsInterpreterVerdict.{Allowed, Denied}
import erules.RuleVerdict.{Allow, Deny, Ignore}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers

import scala.annotation.unused

class RuleResultsInterpreterSpec extends AnyWordSpec with Matchers with EitherValues {

  "EvalResultsInterpreter.Defaults.allowAllNotDenied" should {

    "return Allowed for all not all explicitly denied values" in {

      val interpreter = RuleResultsInterpreter.Defaults.allowAllNotDenied

      val result = interpreter.interpret(
        NonEmptyList.of(
          RuleResult.forRuleName("Ignore").succeeded(Ignore.withoutReasons)
        )
      )

      result shouldBe Allowed(
        NonEmptyList.of(
          RuleResult.noMatch(Allow.allNotExplicitlyDenied)
        )
      )
    }

    "return Allowed for allowed value" in {

      val interpreter = RuleResultsInterpreter.Defaults.allowAllNotDenied

      val result = interpreter.interpret(
        NonEmptyList.of(
          RuleResult.forRuleName("Allow all").succeeded(Allow.withoutReasons)
        )
      )

      result shouldBe Allowed(
        NonEmptyList.of(
          RuleResult.forRuleName("Allow all").succeeded(Allow.withoutReasons)
        )
      )
    }

    "return Denied for if there is at least one Deny" in {

      val interpreter = RuleResultsInterpreter.Defaults.allowAllNotDenied

      val result = interpreter.interpret(
        NonEmptyList.of(
          RuleResult.forRuleName("Allow all").succeeded(Allow.withoutReasons),
          RuleResult.forRuleName("Deny all").succeeded(Deny.withoutReasons)
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          RuleResult.forRuleName("Deny all").succeeded(Deny.withoutReasons)
        )
      )
    }

    "return Denied for if there is at least one evaluated rule in error" in {

      case class Foo()
      val interpreter = RuleResultsInterpreter.Defaults.allowAllNotDenied

      val ex = new RuntimeException("BOOM")

      val allowAll: Rule[EitherThrow, Foo] = Rule("Allow all").failed(ex)

      val result = interpreter.interpret(
        NonEmptyList.one(
          RuleResult.forRule(allowAll).failed(ex)
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          RuleResult.forRule(allowAll).denyForSafetyInCaseOfError(ex)
        )
      )
    }
  }

  "EvalResultsInterpreter.Defaults.denyAllNotAllowed" should {

    "return Denied for all not all explicitly denied values" in {

      val interpreter = RuleResultsInterpreter.Defaults.denyAllNotAllowed

      val result = interpreter.interpret(
        NonEmptyList.of(
          RuleResult.forRuleName("Ignore").succeeded(Ignore.withoutReasons)
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          RuleResult.noMatch(Deny.allNotExplicitlyAllowed)
        )
      )
    }

    "return Allowed for allowed value" in {

      val interpreter = RuleResultsInterpreter.Defaults.denyAllNotAllowed

      val result = interpreter.interpret(
        NonEmptyList.one(
          RuleResult.forRuleName("Allow all").succeeded(Allow.withoutReasons)
        )
      )

      result shouldBe Allowed(
        NonEmptyList.of(
          RuleResult.forRuleName("Allow all").succeeded(Allow.withoutReasons)
        )
      )
    }

    "return Denied for if there is at least one Deny" in {

      case class Foo(@unused x: String, @unused y: Int)
      val interpreter = RuleResultsInterpreter.Defaults.denyAllNotAllowed

      val result = interpreter.interpret(
        NonEmptyList.of(
          RuleResult.forRuleName("Allow all").succeeded(Allow.withoutReasons),
          RuleResult.forRuleName("Deny all").succeeded(Deny.withoutReasons)
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          RuleResult.forRuleName("Deny all").succeeded(Deny.withoutReasons)
        )
      )
    }

    "return Denied for if there is at least one evaluated rule in error" in {

      case class Foo(@unused x: String, @unused y: Int)
      val interpreter = RuleResultsInterpreter.Defaults.denyAllNotAllowed

      val ex = new RuntimeException("BOOM")

      val allowAll: Rule[EitherThrow, Foo] = Rule("Allow all").failed(ex)

      val result = interpreter.interpret(
        NonEmptyList.one(
          RuleResult.forRule(allowAll).failed(ex)
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          RuleResult.forRule(allowAll).denyForSafetyInCaseOfError(ex)
        )
      )
    }
  }
}
