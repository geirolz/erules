package erules.core

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import erules.core.EvalRuleResult.{Allow, Deny}
import erules.core.EvalRulesInterpreterResult.{Allowed, Denied}
import erules.core.RulesEngine.DuplicatedRulesException
import erules.core.testings.{DebuggingIOConsole, ExecutionTimeOps}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.TryValues

import scala.util.{Success, Try}

class RulesEngineSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with ExecutionTimeOps
    with DebuggingIOConsole
    with Matchers
    with TryValues {

  "RulesEngine" should {
    "Return a DuplicatedRulesException with duplicated rules" in {
      case class Foo(x: String, y: Int)
      val allowYEqZero1: Rule[Foo] = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }
      val allowYEqZero2: Rule[Foo] = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      RulesEngine
        .denyAllNotAllowed[Try, Foo](
          NonEmptyList.of(
            allowYEqZero1,
            allowYEqZero2
          )
        )
        .failed
        .get shouldBe a[DuplicatedRulesException]
    }
  }

  //--------------------- EVAL --------------------
  "RulesEngine.denyAllNotAllowed.eval" should {

    "Respond with DENIED when there are no rules for the target" in {

      case class Foo(x: String, y: Int)
      val allowYEqZero = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: RulesEngine[Foo] = RulesEngine
        .denyAllNotAllowed[Try, Foo](NonEmptyList.of(allowYEqZero))
        .get

      val result: IO[EngineResult[Foo]] = engine.parEval(Foo("TEST", 1))

      result.asserting(
        _ shouldBe EngineResult[Foo](
          data = Foo("TEST", 1),
          result = Denied(
            NonEmptyList.of(
              Rule.TypedEvaluated.noMatch(Deny.allNotExplicitlyAllowed)
            )
          )
        )
      )
    }

    "Respond with DENIED when a rule Deny the target" in {

      case class Foo(x: String, y: Int)
      val denyXEqTest = Rule("Check X value").checkOrIgnore[Foo] { case Foo("TEST", _) =>
        Deny.withoutReasons
      }

      val allowYEqZero = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: RulesEngine[Foo] = RulesEngine
        .denyAllNotAllowed[Try, Foo](
          NonEmptyList.of(
            denyXEqTest,
            allowYEqZero
          )
        )
        .get

      val result: IO[EngineResult[Foo]] = engine.parEval(Foo("TEST", 0))

      result.drainExecutionTime
        .asserting(
          _ shouldBe EngineResult[Foo](
            data = Foo("TEST", 0),
            result = Denied(
              NonEmptyList.of(
                Rule.TypedEvaluated(denyXEqTest, Success(EvalRuleResult.Deny.withoutReasons))
              )
            )
          )
        )
    }

    "Respond with ALLOWED when a ALL rules allow the target" in {

      case class Foo(x: String, y: Int)

      val allowYEqZero = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: RulesEngine[Foo] = RulesEngine
        .denyAllNotAllowed[Try, Foo](NonEmptyList.of(allowYEqZero))
        .get

      val result: IO[EngineResult[Foo]] = engine.parEval(Foo("TEST", 0))

      result.drainExecutionTime.asserting(
        _ shouldBe EngineResult[Foo](
          data = Foo("TEST", 0),
          result = Allowed(
            NonEmptyList.of(
              Rule.TypedEvaluated(allowYEqZero, Success(EvalRuleResult.Allow.withoutReasons))
            )
          )
        )
      )
    }
  }

  "RulesEngine.allowAllNotDenied.eval" should {

    "Respond with DENIED for safety in case of rule evaluation error" in {
      case class Foo(x: String, y: Int)

      val ex1 = new RuntimeException("BOOM")
      val ex2 = new RuntimeException("PUFF")

      val allow1 = Rule("ALLOW").asyncCheck[Foo](_ => IO.pure(Allow.withoutReasons))
      val failed1 = Rule("BOOM").asyncCheck[Foo](_ => IO.raiseError(ex1))
      val failed2 = Rule("PUFF").asyncCheck[Foo](_ => IO.raiseError(ex2))

      val engine: RulesEngine[Foo] = RulesEngine
        .denyAllNotAllowed[Try, Foo](
          NonEmptyList.of(
            allow1,
            failed1,
            failed2
          )
        )
        .get

      val result: IO[EngineResult[Foo]] = engine.parEval(Foo("TEST", 1))

      result.drainExecutionTime.asserting(
        _ shouldBe EngineResult[Foo](
          data = Foo("TEST", 1),
          result = Denied(
            NonEmptyList.of(
              Rule.TypedEvaluated.denyForSafetyInCaseOfError(failed1, ex1),
              Rule.TypedEvaluated.denyForSafetyInCaseOfError(failed2, ex2)
            )
          )
        )
      )
    }

    "Respond with ALLOWED when there are no rules for the target" in {

      case class Foo(x: String, y: Int)
      val denyYEqZero = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Deny.withoutReasons
      }

      val engine: RulesEngine[Foo] = RulesEngine
        .allowAllNotDenied[Try, Foo](NonEmptyList.of(denyYEqZero))
        .get

      val result: IO[EngineResult[Foo]] = engine.parEval(Foo("TEST", 1))

      result.asserting(
        _ shouldBe EngineResult[Foo](
          data = Foo("TEST", 1),
          result = Allowed(
            NonEmptyList.of(
              Rule.TypedEvaluated.noMatch(Allow.allNotExplicitlyDenied)
            )
          )
        )
      )
    }

    "Respond with DENIED when a rule Deny the target" in {

      case class Foo(x: String, y: Int)
      val denyXEqTest = Rule("Check X value").checkOrIgnore[Foo] { case Foo("TEST", _) =>
        Deny.withoutReasons
      }

      val allowYEqZero = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: RulesEngine[Foo] = RulesEngine
        .allowAllNotDenied[Try, Foo](
          NonEmptyList.of(
            denyXEqTest,
            allowYEqZero
          )
        )
        .get

      val result: IO[EngineResult[Foo]] = engine.parEval(Foo("TEST", 0))

      result.drainExecutionTime.asserting(
        _ shouldBe EngineResult[Foo](
          data = Foo("TEST", 0),
          result = Denied(
            NonEmptyList.of(
              Rule.TypedEvaluated(denyXEqTest, Success(EvalRuleResult.Deny.withoutReasons))
            )
          )
        )
      )
    }

    "Respond with ALLOWED when a ALL rules allow the target" in {

      case class Foo(x: String, y: Int)

      val allowYEqZero = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: RulesEngine[Foo] = RulesEngine
        .allowAllNotDenied[Try, Foo](NonEmptyList.of(allowYEqZero))
        .get

      val result: IO[EngineResult[Foo]] = engine.parEval(Foo("TEST", 0))

      result.drainExecutionTime.asserting(
        _ shouldBe EngineResult[Foo](
          data = Foo("TEST", 0),
          result = Allowed(
            NonEmptyList.of(
              Rule.TypedEvaluated(allowYEqZero, Success(EvalRuleResult.Allow.withoutReasons))
            )
          )
        )
      )
    }
  }
}
