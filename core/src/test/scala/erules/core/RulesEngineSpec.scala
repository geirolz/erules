package erules.core

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.Id
import erules.core.RuleResultsInterpreterVerdict.{Allowed, Denied}
import erules.core.RulesEngine.DuplicatedRulesException
import erules.core.RuleVerdict.{Allow, Deny}
import erules.core.testings.{ErulesAsyncAssertingSyntax, ReportValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.TryValues

import scala.util.{Success, Try}

class RulesEngineSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers
    with TryValues
    with ErulesAsyncAssertingSyntax
    with ReportValues {

  "RulesEngine" should {
    "Return a DuplicatedRulesException with duplicated rules" in {

      case class Foo(x: String, y: Int)
      val allowYEqZero1: Rule[Id, Foo] = Rule("Check Y value").partially[Id, Foo] {
        case Foo(_, 0) =>
          Allow.withoutReasons
      }

      val allowYEqZero2: PureRule[Foo] = Rule("Check Y value").partially[Id, Foo] {
        case Foo(_, 0) =>
          Allow.withoutReasons
      }

      RulesEngine[Try]
        .withRules(
          allowYEqZero1,
          allowYEqZero2
        )
        .denyAllNotAllowed
        .failed
        .get shouldBe a[DuplicatedRulesException]
    }
  }

  // --------------------- EVAL --------------------
  "RulesEngine.denyAllNotAllowed.eval" should {

    "Respond with DENIED when there are no rules for the target" in {

      case class Foo(x: String, y: Int)
      val allowYEqZero: PureRule[Foo] = Rule("Check Y value").partially[Id, Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: IO[RulesEngineIO[Foo]] =
        RulesEngine[IO]
          .withRules(allowYEqZero)
          .denyAllNotAllowed

      val result: IO[EngineResult[Foo]] = engine.flatMap(_.parEval(Foo("TEST", 1)))

      result.asserting(
        _ shouldBe EngineResult[Foo](
          data = Foo("TEST", 1),
          verdict = Denied(
            NonEmptyList.of(
              RuleResult.noMatch(Deny.allNotExplicitlyAllowed)
            )
          )
        )
      )
    }

    "Respond with DENIED when a rule Deny the target" in {

      case class Foo(x: String, y: Int)
      val denyXEqTest: PureRule[Foo] = Rule("Check X value").partially[Id, Foo] {
        case Foo("TEST", _) =>
          Deny.withoutReasons
      }

      val allowYEqZero: PureRule[Foo] = Rule("Check Y value").partially[Id, Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: IO[RulesEngineIO[Foo]] =
        RulesEngine[IO]
          .withRules(
            denyXEqTest,
            allowYEqZero
          )
          .denyAllNotAllowed

      val result: IO[EngineResult[Foo]] = engine.flatMap(_.parEval(Foo("TEST", 0)))

      result
        .assertingIgnoringTimes(
          _ shouldBe EngineResult[Foo](
            data = Foo("TEST", 0),
            verdict = Denied(
              NonEmptyList.of(
                RuleResult(denyXEqTest, Success(RuleVerdict.Deny.withoutReasons))
              )
            )
          )
        )
    }

    "Respond with ALLOWED when a ALL rules allow the target" in {

      case class Foo(x: String, y: Int)

      val allowYEqZero: Rule[Id, Foo] = Rule("Check Y value").partially[Id, Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: IO[RulesEngineIO[Foo]] =
        RulesEngine[IO]
          .withRules(allowYEqZero)
          .denyAllNotAllowed

      val result: IO[EngineResult[Foo]] = engine.flatMap(_.parEval(Foo("TEST", 0)))

      result.assertingIgnoringTimes(
        _ shouldBe EngineResult[Foo](
          data = Foo("TEST", 0),
          verdict = Allowed(
            NonEmptyList.of(
              RuleResult(allowYEqZero, Success(RuleVerdict.Allow.withoutReasons))
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

      val allow1: RuleIO[Foo]  = Rule("ALLOW").const[IO, Foo](Allow.withoutReasons)
      val failed1: RuleIO[Foo] = Rule("BOOM").failed[IO, Foo](ex1)
      val failed2: RuleIO[Foo] = Rule("PUFF").failed[IO, Foo](ex2)

      val engine: IO[RulesEngineIO[Foo]] =
        RulesEngine[IO]
          .withRules(
            allow1,
            failed1,
            failed2
          )
          .denyAllNotAllowed

      val result: IO[EngineResult[Foo]] = engine.flatMap(_.parEval(Foo("TEST", 1)))

      result.assertingIgnoringTimes(
        _ shouldBe EngineResult[Foo](
          data = Foo("TEST", 1),
          verdict = Denied(
            NonEmptyList.of(
              RuleResult.denyForSafetyInCaseOfError(failed1, ex1),
              RuleResult.denyForSafetyInCaseOfError(failed2, ex2)
            )
          )
        )
      )
    }

    "Respond with ALLOWED when there are no rules for the target" in {

      case class Foo(x: String, y: Int)
      val denyYEqZero: Rule[Id, Foo] = Rule("Check Y value").partially[Id, Foo] { case Foo(_, 0) =>
        Deny.withoutReasons
      }

      val engine: IO[RulesEngineIO[Foo]] =
        RulesEngine[IO]
          .withRules(denyYEqZero)
          .allowAllNotDenied

      val result: IO[EngineResult[Foo]] = engine.flatMap(_.parEval(Foo("TEST", 1)))

      result.asserting(
        _ shouldBe EngineResult[Foo](
          data = Foo("TEST", 1),
          verdict = Allowed(
            NonEmptyList.of(
              RuleResult.noMatch(Allow.allNotExplicitlyDenied)
            )
          )
        )
      )
    }

    "Respond with DENIED when a rule Deny the target" in {

      case class Foo(x: String, y: Int)
      val denyXEqTest: Rule[Id, Foo] = Rule("Check X value").partially[Id, Foo] {
        case Foo("TEST", _) =>
          Deny.withoutReasons
      }

      val allowYEqZero: PureRule[Foo] = Rule("Check Y value").partially[Id, Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: IO[RulesEngineIO[Foo]] =
        RulesEngine[IO]
          .withRules[Id, Foo](
            denyXEqTest,
            allowYEqZero
          )
          .allowAllNotDenied

      val result: IO[EngineResult[Foo]] = engine.flatMap(_.parEval(Foo("TEST", 0)))

      result.assertingIgnoringTimes(
        _ shouldBe EngineResult[Foo](
          data = Foo("TEST", 0),
          verdict = Denied(
            NonEmptyList.of(
              RuleResult(denyXEqTest, Success(RuleVerdict.Deny.withoutReasons))
            )
          )
        )
      )
    }

    "Respond with ALLOWED when a ALL rules allow the target" in {

      case class Foo(x: String, y: Int)

      val allowYEqZero: PureRule[Foo] = Rule("Check Y value").partially[Id, Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: IO[RulesEngineIO[Foo]] =
        RulesEngine[IO]
          .withRules(allowYEqZero)
          .allowAllNotDenied

      val result: IO[EngineResult[Foo]] = engine.flatMap(_.parEval(Foo("TEST", 0)))

      result.assertingIgnoringTimes(
        _ shouldBe EngineResult[Foo](
          data = Foo("TEST", 0),
          verdict = Allowed(
            NonEmptyList.of(
              RuleResult(allowYEqZero, Success(RuleVerdict.Allow.withoutReasons))
            )
          )
        )
      )
    }
  }
}
