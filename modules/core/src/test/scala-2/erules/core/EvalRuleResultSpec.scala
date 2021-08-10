package erules.core

import cats.kernel.Monoid
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.TryValues

class EvalRuleResultSpec extends AnyWordSpec with Matchers with TryValues {

  import EvalRuleResult.*

  "Monoid for EvalRuleResult" should {

    "Return ignore when combining an empty list of results" in {
      Monoid[EvalRuleResult].combineAll(Nil) shouldBe Ignore.withoutReasons
    }

    "combine multiple EvalRuleResult - Allow" in {

      //allow-allow
      Monoid[EvalRuleResult].combineAll(
        Seq(
          Allow.because("R1"),
          Allow.because("R2")
        )
      ) shouldBe Allow.because("R1").because("R2")

      //allow-deny
      Monoid[EvalRuleResult].combineAll(
        Seq(
          Allow.because("R1"),
          Deny.because("R2")
        )
      ) shouldBe Deny.because("R2")

      //allow-ignore
      Monoid[EvalRuleResult].combineAll(
        Seq(
          Allow.because("R1"),
          Ignore.because("R2")
        )
      ) shouldBe Allow.because("R1")
    }

    "combine multiple EvalRuleResult - Deny" in {
      //deny-deny
      Monoid[EvalRuleResult].combineAll(
        Seq(
          Deny.because("R1"),
          Deny.because("R2")
        )
      ) shouldBe Deny.because("R1").because("R2")

      //deny-allow
      Monoid[EvalRuleResult].combineAll(
        Seq(
          Deny.because("R1"),
          Allow.because("R2")
        )
      ) shouldBe Deny.because("R1")

      //deny-ignore
      Monoid[EvalRuleResult].combineAll(
        Seq(
          Deny.because("R1"),
          Ignore.because("R2")
        )
      ) shouldBe Deny.because("R1")
    }

    "combine multiple EvalRuleResult - Ignore" in {

      //ignore-ignore
      Monoid[EvalRuleResult].combineAll(
        Seq(
          Ignore.because("R1"),
          Ignore.because("R2")
        )
      ) shouldBe Ignore.because("R1").because("R2")

      //ignore-allow
      Monoid[EvalRuleResult].combineAll(
        Seq(
          Ignore.because("R1"),
          Allow.because("R2")
        )
      ) shouldBe Allow.because("R2")

      //ignore-deny
      Monoid[EvalRuleResult].combineAll(
        Seq(
          Ignore.because("R1"),
          Deny.because("R2")
        )
      ) shouldBe Deny.because("R2")
    }
  }
}
