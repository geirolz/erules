package erules.core

import cats.data.NonEmptyList
import erules.core.RuleResultsInterpreterVerdict.{Allowed, Denied}
import erules.core.RuleVerdict.{Allow, Deny, Ignore}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.TryValues
import org.scalatest.matchers.should.Matchers

import scala.util.Failure

class RuleResultsInterpreterSpec extends AnyWordSpec with Matchers with TryValues {

  "EvalResultsInterpreter.Defaults.allowAllNotDenied" should {

    "return Allowed for all not all explicitly denied values" in {

      case class Foo(x: String, y: Int)
      val interpreter = RuleResultsInterpreter.Defaults.allowAllNotDenied

      val result = interpreter.interpret(
        NonEmptyList.of(
          RuleResult.const("Ignore", Ignore.withoutReasons)
        )
      )

      result shouldBe Allowed(
        NonEmptyList.of(
          RuleResult.noMatch(Allow.allNotExplicitlyDenied)
        )
      )
    }

    "return Allowed for allowed value" in {

      case class Foo(x: String, y: Int)
      val interpreter = RuleResultsInterpreter.Defaults.allowAllNotDenied

      val result = interpreter.interpret(
        NonEmptyList.of(
          RuleResult.const("Allow all", Allow.withoutReasons)
        )
      )

      result shouldBe Allowed(
        NonEmptyList.of(
          RuleResult.const("Allow all", Allow.withoutReasons)
        )
      )
    }

    "return Denied for if there is at least one Deny" in {

      case class Foo(x: String, y: Int)
      val interpreter = RuleResultsInterpreter.Defaults.allowAllNotDenied

      val result = interpreter.interpret(
        NonEmptyList.of(
          RuleResult.const("Allow all", Allow.withoutReasons),
          RuleResult.const("Deny all", Deny.withoutReasons)
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          RuleResult.const("Deny all", Deny.withoutReasons)
        )
      )
    }

    "return Denied for if there is at least one evaluated rule in error" in {

      case class Foo(x: String, y: Int)
      val interpreter = RuleResultsInterpreter.Defaults.allowAllNotDenied

      val ex = new RuntimeException("BOOM")

      val allowAll: Rule[Foo] = Rule("Allow all").failed(ex)

      val result = interpreter.interpret(
        NonEmptyList.one(
          RuleResult(allowAll, Failure(ex))
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          RuleResult.denyForSafetyInCaseOfError(allowAll, ex)
        )
      )
    }
  }

  "EvalResultsInterpreter.Defaults.denyAllNotAllowed" should {

    "return Denied for all not all explicitly denied values" in {

      case class Foo(x: String, y: Int)
      val interpreter = RuleResultsInterpreter.Defaults.denyAllNotAllowed

      val result = interpreter.interpret(
        NonEmptyList.of(
          RuleResult.const("Ignore", Ignore.withoutReasons)
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          RuleResult.noMatch(Deny.allNotExplicitlyAllowed)
        )
      )
    }

    "return Allowed for allowed value" in {

      case class Foo(x: String, y: Int)
      val interpreter = RuleResultsInterpreter.Defaults.denyAllNotAllowed

      val result = interpreter.interpret(
        NonEmptyList.one(
          RuleResult.const("Allow all", Allow.withoutReasons)
        )
      )

      result shouldBe Allowed(
        NonEmptyList.of(
          RuleResult.const("Allow all", Allow.withoutReasons)
        )
      )
    }

    "return Denied for if there is at least one Deny" in {

      case class Foo(x: String, y: Int)
      val interpreter = RuleResultsInterpreter.Defaults.denyAllNotAllowed

      val result = interpreter.interpret(
        NonEmptyList.of(
          RuleResult.const("Allow all", Allow.withoutReasons),
          RuleResult.const("Deny all", Deny.withoutReasons)
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          RuleResult.const("Deny all", Deny.withoutReasons)
        )
      )
    }

    "return Denied for if there is at least one evaluated rule in error" in {

      case class Foo(x: String, y: Int)
      val interpreter = RuleResultsInterpreter.Defaults.denyAllNotAllowed

      val ex = new RuntimeException("BOOM")

      val allowAll: Rule[Foo] = Rule("Allow all").failed(ex)

      val result = interpreter.interpret(
        NonEmptyList.one(
          RuleResult(allowAll, Failure(ex))
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          RuleResult.denyForSafetyInCaseOfError(allowAll, ex)
        )
      )
    }
  }
}
