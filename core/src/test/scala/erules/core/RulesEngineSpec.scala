package erules.core

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.Id
import erules.core.RuleResultsInterpreterVerdict.{Allowed, Denied}
import erules.core.RulesEngine.DuplicatedRulesException
import erules.core.RuleVerdict.{Allow, Deny}
import erules.core.testings.ErulesAsyncAssertingSyntax
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.annotation.unused
import scala.util.Try

class RulesEngineSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers
    with ErulesAsyncAssertingSyntax {

  case class Foo(@unused x: String, @unused y: Int)

  "RulesEngine" should {
    "Return a DuplicatedRulesException with duplicated rules" in {

      val allowYEqZero1: Rule[Id, Foo] = Rule("Check Y value").partially[Id, Foo] {
        case Foo(_, 0) =>
          Allow.withoutReasons
      }

      val allowYEqZero2: PureRule[Foo] = Rule("Check Y value").partially[Id, Foo] {
        case Foo(_, 0) =>
          Allow.withoutReasons
      }

      assert(
        RulesEngine[Try]
          .withRules(
            allowYEqZero1,
            allowYEqZero2
          )
          .denyAllNotAllowed
          .failed
          .isSuccess
      )
    }
  }

  // --------------------- EVAL --------------------
  "RulesEngine.denyAllNotAllowed.eval" should {

    "Respond with DENIED when there are no rules for the target" in {

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

      val denyXEqTest: PureRule[Foo] = Rule("Check X value").partially[Id, Foo] {
        case Foo("TEST", _) => Deny.withoutReasons
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
                RuleResult
                  .forRule(denyXEqTest)
                  .succeeded(RuleVerdict.Deny.withoutReasons)
              )
            )
          )
        )
    }

    "Respond with ALLOWED when a ALL rules allow the target" in {

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
              RuleResult.forRule(allowYEqZero).succeeded(RuleVerdict.Allow.withoutReasons)
            )
          )
        )
      )
    }
  }

  "RulesEngine.allowAllNotDenied.eval" should {

    "Respond with DENIED for safety in case of rule evaluation error" in {

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
              RuleResult.forRule(failed1).denyForSafetyInCaseOfError(ex1),
              RuleResult.forRule(failed2).denyForSafetyInCaseOfError(ex2)
            )
          )
        )
      )
    }

    "Respond with ALLOWED when there are no rules for the target" in {

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
              RuleResult.forRule(denyXEqTest).succeeded(RuleVerdict.Deny.withoutReasons)
            )
          )
        )
      )
    }

    "Respond with ALLOWED when a ALL rules allow the target" in {

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
              RuleResult.forRule(allowYEqZero).succeeded(RuleVerdict.Allow.withoutReasons)
            )
          )
        )
      )
    }
  }
}
