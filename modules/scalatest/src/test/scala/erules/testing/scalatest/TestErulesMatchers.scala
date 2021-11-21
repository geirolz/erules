package erules.testing.scalatest

import cats.data.NonEmptyList
import cats.effect.testing.scalatest.AsyncIOSpec
import erules.core.RuleResult
import erules.core.RuleResultsInterpreterVerdict.{Allowed, Denied}
import erules.core.RuleVerdict.{Allow, Deny, Ignore}
import erules.testing.scaltest.ErulesMatchers
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

class TestErulesMatchers extends AsyncFunSuite with AsyncIOSpec with ErulesMatchers with Matchers {

  test("RuleResultsInterpreterVerdict should be allowed and should not be denied") {

    val verdict: Allowed[Nothing] = Allowed(
      NonEmptyList.of(
        RuleResult.const("Foo", Allow.withoutReasons)
      )
    )

    verdict shouldBe allowed
    verdict should not be denied
  }

  test("RuleResultsInterpreterVerdict should be denied and should not be allowed") {

    val verdict: Denied[Nothing] = Denied(
      NonEmptyList.of(
        RuleResult.const("Foo", Deny.withoutReasons)
      )
    )

    verdict shouldBe denied
    verdict should not be allowed
  }

  test("RuleVerdict should be allow and should not be deny or ignore") {

    val verdict = Allow.withoutReasons

    verdict shouldBe allow
    verdict should not be deny
    verdict should not be ignore
  }

  test("RuleVerdict should be deny and should not be allow or ignore") {

    val verdict = Deny.withoutReasons

    verdict shouldBe deny
    verdict should not be allow
    verdict should not be ignore
  }

  test("RuleVerdict should be ignore and should not be allow or deny") {

    val verdict = Ignore.withoutReasons

    verdict shouldBe ignore
    verdict should not be allow
    verdict should not be deny
  }
}
