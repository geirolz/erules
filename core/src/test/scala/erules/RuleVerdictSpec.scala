package erules

import cats.kernel.Monoid
import erules.RuleVerdict.{Allow, Deny, Ignore}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues

class RuleVerdictSpec extends AnyWordSpec with Matchers with EitherValues {

  "Monoid for RuleVerdict" should {

    "Return ignore when combining an empty list of results" in {
      Monoid[RuleVerdict].combineAll(Nil) shouldBe Ignore.withoutReasons
    }

    "combine multiple RuleVerdict - Allow" in {

      // allow-allow
      Monoid[RuleVerdict].combineAll(
        Seq(
          Allow.because("R1"),
          Allow.because("R2")
        )
      ) shouldBe Allow.because("R1").because("R2")

      // allow-deny
      Monoid[RuleVerdict].combineAll(
        Seq(
          Allow.because("R1"),
          Deny.because("R2")
        )
      ) shouldBe Deny.because("R2")

      // allow-ignore
      Monoid[RuleVerdict].combineAll(
        Seq(
          Allow.because("R1"),
          Ignore.because("R2")
        )
      ) shouldBe Allow.because("R1")
    }

    "combine multiple RuleVerdict - Deny" in {
      // deny-deny
      Monoid[RuleVerdict].combineAll(
        Seq(
          Deny.because("R1"),
          Deny.because("R2")
        )
      ) shouldBe Deny.because("R1").because("R2")

      // deny-allow
      Monoid[RuleVerdict].combineAll(
        Seq(
          Deny.because("R1"),
          Allow.because("R2")
        )
      ) shouldBe Deny.because("R1")

      // deny-ignore
      Monoid[RuleVerdict].combineAll(
        Seq(
          Deny.because("R1"),
          Ignore.because("R2")
        )
      ) shouldBe Deny.because("R1")
    }

    "combine multiple RuleVerdict - Ignore" in {

      // ignore-ignore
      Monoid[RuleVerdict].combineAll(
        Seq(
          Ignore.because("R1"),
          Ignore.because("R2")
        )
      ) shouldBe Ignore.because("R1").because("R2")

      // ignore-allow
      Monoid[RuleVerdict].combineAll(
        Seq(
          Ignore.because("R1"),
          Allow.because("R2")
        )
      ) shouldBe Allow.because("R2")

      // ignore-deny
      Monoid[RuleVerdict].combineAll(
        Seq(
          Ignore.because("R1"),
          Deny.because("R2")
        )
      ) shouldBe Deny.because("R2")
    }
  }
}
