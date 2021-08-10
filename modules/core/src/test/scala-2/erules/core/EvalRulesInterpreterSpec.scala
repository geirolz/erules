package erules.core

import cats.data.NonEmptyList
import cats.effect.IO
import erules.core.EvalRuleResult.{Allow, Deny, Ignore}
import erules.core.EvalRulesInterpreterResult.{Allowed, Denied}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.TryValues
import org.scalatest.matchers.should.Matchers

import scala.util.{Failure, Success}

class EvalRulesInterpreterSpec extends AnyWordSpec with Matchers with TryValues {

  "EvalResultsInterpreter.Defaults.allowAllNotDenied" should {

    "return Allowed for all not all explicitly denied values" in {

      case class Foo(x: String, y: Int)
      val interpreter = EvalRulesInterpreter.Defaults.allowAllNotDenied

      val result = interpreter.interpret(
        NonEmptyList.of(
          Rule.TypedEvaluated(
            Rule("Ignore").const[Foo](Ignore.withoutReasons),
            Success(Ignore.withoutReasons)
          )
        )
      )

      result shouldBe Allowed(
        NonEmptyList.of(
          Rule.TypedEvaluated.noMatch(Allow.allNotExplicitlyDenied)
        )
      )
    }

    "return Allowed for allowed value" in {

      case class Foo(x: String, y: Int)
      val interpreter = EvalRulesInterpreter.Defaults.allowAllNotDenied

      val allowAll: Rule[Foo] = Rule("Allow all").const[Foo](Allow.withoutReasons)

      val result = interpreter.interpret(
        NonEmptyList.of(
          Rule.TypedEvaluated(allowAll, Success(Allow.withoutReasons))
        )
      )

      result shouldBe Allowed(
        NonEmptyList.of(
          Rule.TypedEvaluated(allowAll, Success(Allow.withoutReasons))
        )
      )
    }

    "return Denied for if there is at least one Deny" in {

      case class Foo(x: String, y: Int)
      val interpreter = EvalRulesInterpreter.Defaults.allowAllNotDenied

      val allowAll: Rule[Foo] = Rule("Allow all").const[Foo](Allow.withoutReasons)
      val denyAll: Rule[Foo] = Rule("Deny all").const[Foo](Deny.withoutReasons)

      val result = interpreter.interpret(
        NonEmptyList.of(
          Rule.TypedEvaluated(allowAll, Success(Allow.withoutReasons)),
          Rule.TypedEvaluated(denyAll, Success(Deny.withoutReasons))
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          Rule.TypedEvaluated(denyAll, Success(Deny.withoutReasons))
        )
      )
    }

    "return Denied for if there is at least one evaluated rule in error" in {

      case class Foo(x: String, y: Int)
      val interpreter = EvalRulesInterpreter.Defaults.allowAllNotDenied

      val ex = new RuntimeException("BOOM")

      val allowAll: Rule[Foo] = Rule("Allow all").asyncCheck[Foo](_ => IO.raiseError(ex))

      val result = interpreter.interpret(
        NonEmptyList.one(
          Rule.TypedEvaluated(allowAll, Failure(ex))
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          Rule.TypedEvaluated.denyForSafetyInCaseOfError(allowAll, ex)
        )
      )
    }
  }

  "EvalResultsInterpreter.Defaults.denyAllNotAllowed" should {

    "return Denied for all not all explicitly denied values" in {

      case class Foo(x: String, y: Int)
      val interpreter = EvalRulesInterpreter.Defaults.denyAllNotAllowed

      val result = interpreter.interpret(
        NonEmptyList.of(
          Rule.TypedEvaluated(
            Rule("Ignore").const[Foo](Ignore.withoutReasons),
            Success(Ignore.withoutReasons)
          )
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          Rule.TypedEvaluated.noMatch(Deny.allNotExplicitlyAllowed)
        )
      )
    }

    "return Allowed for allowed value" in {

      case class Foo(x: String, y: Int)
      val interpreter = EvalRulesInterpreter.Defaults.denyAllNotAllowed

      val allowAll: Rule[Foo] = Rule("Allow all").const[Foo](Allow.withoutReasons)

      val result = interpreter.interpret(
        NonEmptyList.one(
          Rule.TypedEvaluated(allowAll, Success(Allow.withoutReasons))
        )
      )

      result shouldBe Allowed(
        NonEmptyList.of(
          Rule.TypedEvaluated(allowAll, Success(Allow.withoutReasons))
        )
      )
    }

    "return Denied for if there is at least one Deny" in {

      case class Foo(x: String, y: Int)
      val interpreter = EvalRulesInterpreter.Defaults.denyAllNotAllowed

      val allowAll: Rule[Foo] = Rule("Allow all").const[Foo](Allow.withoutReasons)
      val denyAll: Rule[Foo] = Rule("Deny all").const[Foo](Deny.withoutReasons)

      val result = interpreter.interpret(
        NonEmptyList.of(
          Rule.TypedEvaluated(allowAll, Success(Allow.withoutReasons)),
          Rule.TypedEvaluated(denyAll, Success(Deny.withoutReasons))
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          Rule.TypedEvaluated(denyAll, Success(Deny.withoutReasons))
        )
      )
    }

    "return Denied for if there is at least one evaluated rule in error" in {

      case class Foo(x: String, y: Int)
      val interpreter = EvalRulesInterpreter.Defaults.denyAllNotAllowed

      val ex = new RuntimeException("BOOM")

      val allowAll: Rule[Foo] = Rule("Allow all").asyncCheck[Foo](_ => IO.raiseError(ex))

      val result = interpreter.interpret(
        NonEmptyList.one(
          Rule.TypedEvaluated(allowAll, Failure(ex))
        )
      )

      result shouldBe Denied(
        NonEmptyList.of(
          Rule.TypedEvaluated.denyForSafetyInCaseOfError(allowAll, ex)
        )
      )
    }
  }
}
