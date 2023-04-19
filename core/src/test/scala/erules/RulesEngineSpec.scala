package erules

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import erules.RuleResultsInterpreterVerdict.{Allowed, Denied}
import erules.RuleVerdict.{Allow, Deny}
import erules.testings.ErulesAsyncAssertingSyntax
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

      val allowYEqZero1: PureRule[Foo] = Rule("Check Y value").partially { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val allowYEqZero2: PureRule[Foo] = Rule("Check Y value").partially { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      assert(
        RulesEngine
          .withRules(
            allowYEqZero1,
            allowYEqZero2
          )
          .liftK[IO]
          .denyAllNotAllowed[Try]
          .failed
          .isSuccess
      )
    }
  }

  // --------------------- EVAL --------------------
  "RulesEngine.denyAllNotAllowed.eval" should {

    "Respond with DENIED when there are no rules for the target" in {

      val allowYEqZero: PureRule[Foo] = Rule("Check Y value").partially { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: IO[RulesEngineIO[Foo]] =
        RulesEngine
          .withRules(allowYEqZero)
          .liftK[IO]
          .denyAllNotAllowed[IO]

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

      val denyXEqTest: PureRule[Foo] = Rule("Check X value").partially { case Foo("TEST", _) =>
        Deny.withoutReasons
      }

      val allowYEqZero: PureRule[Foo] = Rule("Check Y value").partially { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: IO[RulesEngineIO[Foo]] =
        RulesEngine
          .withRules(
            denyXEqTest,
            allowYEqZero
          )
          .liftK[IO]
          .denyAllNotAllowed[IO]

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

      val allowYEqZero: PureRule[Foo] = Rule("Check Y value").partially { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: IO[RulesEngineIO[Foo]] =
        RulesEngine
          .withRules(allowYEqZero)
          .liftK[IO]
          .denyAllNotAllowed[IO]

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

      val allow1: RuleIO[Foo]  = Rule("ALLOW").const(Allow.withoutReasons)
      val failed1: RuleIO[Foo] = Rule("BOOM").failed(ex1)
      val failed2: RuleIO[Foo] = Rule("PUFF").failed(ex2)

      val engine: IO[RulesEngineIO[Foo]] =
        RulesEngine
          .withRules(
            allow1,
            failed1,
            failed2
          )
          .denyAllNotAllowed[IO]

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

      val denyYEqZero: PureRule[Foo] = Rule("Check Y value").partially { case Foo(_, 0) =>
        Deny.withoutReasons
      }

      val engine: IO[RulesEngineIO[Foo]] =
        RulesEngine
          .withRules(denyYEqZero)
          .liftK[IO]
          .allowAllNotDenied[IO]

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

      val denyXEqTest: PureRule[Foo] = Rule("Check X value").partially { case Foo("TEST", _) =>
        Deny.withoutReasons
      }

      val allowYEqZero: PureRule[Foo] = Rule("Check Y value").partially { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: IO[RulesEngineIO[Foo]] =
        RulesEngine
          .withRules(
            denyXEqTest,
            allowYEqZero
          )
          .liftK[IO]
          .allowAllNotDenied[IO]

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

      val allowYEqZero: PureRule[Foo] = Rule("Check Y value").partially { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: IO[PureRulesEngine[Foo]] =
        RulesEngine
          .withRules(allowYEqZero)
          .allowAllNotDenied[IO]

      val result: IO[EngineResult[Foo]] = engine.map(_.seqEvalPure(Foo("TEST", 0)))

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
