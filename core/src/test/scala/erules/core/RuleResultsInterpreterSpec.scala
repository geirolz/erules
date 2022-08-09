package erules.core

import cats.data.NonEmptyList
import erules.core.RuleResultsInterpreterVerdict.{Allowed, Denied}
import erules.core.RuleVerdict.{Allow, Deny, Ignore}
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

      case class Foo()
      val interpreter = RuleResultsInterpreter.Defaults.allowAllNotDenied

      val ex = new RuntimeException("BOOM")

      val allowAll: Rule[EitherThrow, Foo] = Rule("Allow all").failed[EitherThrow, Foo](ex)

      val result = interpreter.interpret(
        NonEmptyList.one(
          RuleResult(allowAll, Left(ex))
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

      case class Foo(@unused x: String, @unused y: Int)
      val interpreter = RuleResultsInterpreter.Defaults.denyAllNotAllowed

      val result = interpreter.interpret(
        NonEmptyList.of(
          RuleResult.const("Allow all", Allow.withoutReasons),
          RuleResult.const("Deny all", Deny.withoutReasons)
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          RuleResult.const[Foo, Deny]("Deny all", Deny.withoutReasons)
        )
      )
    }

    "return Denied for if there is at least one evaluated rule in error" in {

      case class Foo(@unused x: String, @unused y: Int)
      val interpreter = RuleResultsInterpreter.Defaults.denyAllNotAllowed

      val ex = new RuntimeException("BOOM")

      val allowAll: Rule[EitherThrow, Foo] = Rule("Allow all").failed[EitherThrow, Foo](ex)

      val result = interpreter.interpret(
        NonEmptyList.one(
          RuleResult(allowAll, Left(ex))
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
