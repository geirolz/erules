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

  test(
    "RuleResult - ErulesAsyncAssertingSyntax.assertingIgnoringTimes should drain execution times"
  ) {
    IO(
      RuleResult
        .forRuleName("Allow all")
        .succeeded(RuleVerdict.Allow.withoutReasons, Some(1.seconds))
    ).assertingIgnoringTimes(
      _ shouldBe RuleResult.forRuleName("Allow all").succeeded(RuleVerdict.Allow.withoutReasons)
    )
  }

  test(
    "EngineResult - ErulesAsyncAssertingSyntax.assertingIgnoringTimes should drain execution times"
  ) {
    IO(
      EngineResult(
        data = (),
        verdict = Allowed(
          NonEmptyList.of(
            RuleResult
              .forRuleName("Allow all 1")
              .succeeded(RuleVerdict.Allow.withoutReasons, Some(1.seconds)),
            RuleResult
              .forRuleName("Allow all 2")
              .succeeded(RuleVerdict.Allow.withoutReasons, Some(2.seconds))
          )
        )
      )
    ).assertingIgnoringTimes(
      _ shouldBe EngineResult(
        data = (),
        verdict = Allowed(
          NonEmptyList.of(
            RuleResult
              .forRuleName("Allow all 1")
              .succeeded(RuleVerdict.Allow.withoutReasons),
            RuleResult
              .forRuleName("Allow all 2")
              .succeeded(RuleVerdict.Allow.withoutReasons)
          )
        )
      )
    )
  }
}
