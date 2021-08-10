package erules.testing.scalatest

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import erules.core.{EngineResult, RuleResult, RuleVerdict}
import erules.core.RuleResultsInterpreterVerdict.Allowed
import erules.testing.scaltest.ErulesAsyncAssertingSyntax
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

class TestErulesAsyncAssertingSyntax
    extends AsyncFunSuite
    with AsyncIOSpec
    with ErulesAsyncAssertingSyntax
    with Matchers {

  test("RuleResult - ErulesAsyncAssertingSyntax.assertingIgnoringTimes should drain execution times") {
    IO(
      RuleResult
        .const("Allow all", RuleVerdict.Allow.withoutReasons)
        .copy(executionTime = Some(1.seconds))
    ).assertingIgnoringTimes(
      _ shouldBe RuleResult.const("Allow all", RuleVerdict.Allow.withoutReasons)
    )
  }

  test("EngineResult - ErulesAsyncAssertingSyntax.assertingIgnoringTimes should drain execution times") {
    IO(
      EngineResult(
        data = (),
        verdict = Allowed(
          NonEmptyList.of(
            RuleResult
              .const("Allow all 1", RuleVerdict.Allow.withoutReasons)
              .copy(executionTime = Some(1.seconds)),
            RuleResult
              .const("Allow all 2", RuleVerdict.Allow.withoutReasons)
              .copy(executionTime = Some(2.seconds))
          )
        )
      )
    ).assertingIgnoringTimes(
      _ shouldBe EngineResult(
        data = (),
        verdict = Allowed(
          NonEmptyList.of(
            RuleResult
              .const("Allow all 1", RuleVerdict.Allow.withoutReasons),
            RuleResult
              .const("Allow all 2", RuleVerdict.Allow.withoutReasons)
          )
        )
      )
    )
  }
}
