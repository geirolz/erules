package erules.core

import cats.data.NonEmptyList
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO
import erules.core.EvalRuleResult.{Allow, Deny, Ignore}
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.TryValues
import org.scalatest.matchers.should.Matchers

import scala.util.{Failure, Success}

class RuleSpec extends AsyncWordSpec with AsyncIOSpec with Matchers with TryValues {

  //------------------------- CONTRAMAP -------------------------
  "Rule.contramap" should {
    "Apply the contravariant map widening the type" in {

      case class Foo(bar: Bar)
      case class Bar(baz: Baz)
      case class Baz(value: String)

      val bazRule: Rule[Baz] = Rule("Check Baz value").check[Baz] {
        _.value match {
          case "" => Deny.because("Empty value")
          case _  => Allow.withoutReasons
        }
      }

      val fooRule: Rule[Foo] = bazRule.contramap(_.bar.baz)

      bazRule.eval(Baz("")).asserting(_ shouldBe Deny.because("Empty value"))
      bazRule.eval(Baz("baz")).asserting(_ shouldBe Allow.withoutReasons)

      fooRule.eval(Foo(Bar(Baz("")))).asserting(_ shouldBe Deny.because("Empty value"))
      fooRule.eval(Foo(Bar(Baz("bar")))).asserting(_ shouldBe Allow.withoutReasons)
    }
  }

  //------------------------- EVAL ZIP -------------------------
  "Rule.asyncCheck.evalZip" should {
    "return the right result once evaluated" in {

      sealed trait ADT
      case class Foo(x: String, y: Int) extends ADT
      case class Bar(x: String, y: Int) extends ADT

      val rule: Rule[ADT] = Rule("Check Y value").asyncCheck[ADT] {
        case Foo(_, 0) => IO.pure(Allow.withoutReasons)
        case Bar(_, 1) => IO.pure(Deny.withoutReasons)
        case _         => IO.pure(Ignore.withoutReasons)
      }

      rule.evalZip(Foo("TEST", 0)).asserting(_ shouldBe Rule.TypedEvaluated(rule, Success(Allow.withoutReasons)))
      rule.evalZip(Bar("TEST", 1)).asserting(_ shouldBe Rule.TypedEvaluated(rule, Success(Deny.withoutReasons)))
    }

    "return an exception when a case fail" in {

      sealed trait ADT
      case class Foo(x: String, y: Int) extends ADT
      case class Bar(x: String, y: Int) extends ADT

      val ex = new RuntimeException("BOOM")

      val rule: Rule[ADT] = Rule("Check Y value").asyncCheck[ADT] {
        case Foo(_, 0) => IO.raiseError(ex)
        case Bar(_, 1) => IO.pure(Deny.withoutReasons)
        case _         => IO.pure(Ignore.withoutReasons)

      }

      rule.evalZip(Foo("TEST", 0)).asserting(_ shouldBe Rule.TypedEvaluated(rule, Failure(ex)))
      rule.evalZip(Bar("TEST", 1)).asserting(_ shouldBe Rule.TypedEvaluated(rule, Success(Deny.withoutReasons)))
    }
  }

  "Rule.asyncCheckOrIgnore.evalZip" should {
    "return the right result once evaluated" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").asyncCheckOrIgnore[Foo] {
        case Foo(_, 0) => IO.pure(Allow.withoutReasons)
        case Foo(_, 1) => IO.pure(Deny.withoutReasons)
      }

      rule.evalZip(Foo("TEST", 0)).asserting(_ shouldBe Rule.TypedEvaluated(rule, Success(Allow.withoutReasons)))
      rule.evalZip(Foo("TEST", 1)).asserting(_ shouldBe Rule.TypedEvaluated(rule, Success(Deny.withoutReasons)))
    }

    "return an exception when a case fail" in {
      case class Foo(x: String, y: Int)
      val ex = new RuntimeException("BOOM")

      val rule: Rule[Foo] = Rule("Check Y value").asyncCheckOrIgnore[Foo] {
        case Foo(_, 0) => IO.raiseError(ex)
        case Foo(_, 1) => IO.pure(Deny.withoutReasons)
      }

      rule.evalZip(Foo("TEST", 0)).asserting(_ shouldBe Rule.TypedEvaluated(rule, Failure(ex)))
      rule.evalZip(Foo("TEST", 1)).asserting(_ shouldBe Rule.TypedEvaluated(rule, Success(Deny.withoutReasons)))
    }
  }

  "Rule.check.evalZip" should {
    "return the right result once evaluated when exhaustive" in {
      sealed trait ADT
      case class Foo() extends ADT
      case class Bar() extends ADT

      val rule: Rule[ADT] = Rule("Check Y value").check[ADT] {
        case Foo() => Allow.withoutReasons
        case Bar() => Deny.withoutReasons
      }

      rule.evalZip(Foo()).asserting(_ shouldBe Rule.TypedEvaluated(rule, Success(Allow.withoutReasons)))
      rule.evalZip(Bar()).asserting(_ shouldBe Rule.TypedEvaluated(rule, Success(Deny.withoutReasons)))
    }
  }

  "Rule.checkOrIgnore.evalZip" should {
    "return the right result once evaluated in defined domain" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      rule.evalZip(Foo("TEST", 0)).asserting(_ shouldBe Rule.TypedEvaluated(rule, Success(Allow.withoutReasons)))
    }

    "return the Ignore once evaluated out of the defined domain" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      rule.evalZip(Foo("TEST", 1)).asserting(_ shouldBe Rule.TypedEvaluated(rule, Success(Ignore.noMatch)))
    }
  }

  //------------------------- EVAL -------------------------
  "Rule.asyncCheck.eval" should {
    "return the right result once evaluated" in {

      sealed trait ADT
      case class Foo(x: String, y: Int) extends ADT
      case class Bar(x: String, y: Int) extends ADT

      val rule: Rule[ADT] = Rule("Check Y value").asyncCheck[ADT] {
        case Foo(_, 0) => IO.pure(Allow.withoutReasons)
        case Bar(_, 1) => IO.pure(Deny.withoutReasons)
        case _         => IO.pure(Ignore.withoutReasons)
      }

      rule.eval(Foo("TEST", 0)).asserting(_ shouldBe EvalRuleResult.Allow.withoutReasons)
      rule.eval(Bar("TEST", 1)).asserting(_ shouldBe EvalRuleResult.Deny.withoutReasons)
    }

    "return an exception when a case fail" in {

      sealed trait ADT
      case class Foo(x: String, y: Int) extends ADT
      case class Bar(x: String, y: Int) extends ADT

      val rule: Rule[ADT] = Rule("Check Y value").asyncCheck[ADT] {
        case Foo(_, 0) => IO.raiseError(new RuntimeException("BOOM"))
        case Bar(_, 1) => IO.pure(Deny.withoutReasons)
        case _         => IO.pure(Ignore.withoutReasons)
      }

      rule.eval(Foo("TEST", 0)).assertThrows[RuntimeException]
      rule.eval(Bar("TEST", 1)).asserting(_ shouldBe EvalRuleResult.Deny.withoutReasons)
    }
  }

  "Rule.asyncCheckOrIgnore.eval" should {
    "return the right result once evaluated" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").asyncCheckOrIgnore[Foo] {
        case Foo(_, 0) => IO.pure(Allow.withoutReasons)
        case Foo(_, 1) => IO.pure(Deny.withoutReasons)
      }

      rule.eval(Foo("TEST", 0)).asserting(_ shouldBe EvalRuleResult.Allow.withoutReasons)
      rule.eval(Foo("TEST", 1)).asserting(_ shouldBe EvalRuleResult.Deny.withoutReasons)
    }

    "return an exception when a case fail" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").asyncCheckOrIgnore[Foo] {
        case Foo(_, 0) => IO.raiseError(new RuntimeException("BOOM"))
        case Foo(_, 1) => IO.pure(Deny.withoutReasons)
      }

      rule.eval(Foo("TEST", 0)).assertThrows[RuntimeException]
      rule.eval(Foo("TEST", 1)).asserting(_ shouldBe EvalRuleResult.Deny.withoutReasons)
    }
  }

  "Rule.check.eval" should {
    "return the right result once evaluated when exhaustive" in {
      sealed trait ADT
      case class Foo() extends ADT
      case class Bar() extends ADT

      val rule: Rule[ADT] = Rule("Check Y value").check[ADT] {
        case Foo() => Allow.withoutReasons
        case Bar() => Deny.withoutReasons
      }

      rule.eval(Foo()).asserting(_ shouldBe EvalRuleResult.Allow.withoutReasons)
      rule.eval(Bar()).asserting(_ shouldBe EvalRuleResult.Deny.withoutReasons)
    }
  }

  "Rule.checkOrIgnore.eval" should {
    "return the right result once evaluated in defined domain" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      rule.eval(Foo("TEST", 0)).asserting(_ shouldBe EvalRuleResult.Allow.withoutReasons)
    }

    "return the Ignore once evaluated out of the defined domain" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      rule.eval(Foo("TEST", 1)).asserting(_ shouldBe EvalRuleResult.Ignore.noMatch)
    }
  }

  //------------------------- UTILS -------------------------
  "Rule.findDuplicated" should {
    "return the list of duplicated rules" in {
      case class Foo(x: String, y: Int)

      val duplicated: List[Rule[Foo]] = Rule.findDuplicated(
        NonEmptyList.of(
          Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
            Allow.withoutReasons
          },
          Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 1) =>
            Allow.withoutReasons
          }
        )
      )

      duplicated.map(_.name) shouldBe List("Check Y value")
    }

    "return a Nil when there are no duplicated descriptions" in {
      case class Foo(x: String, y: Int)

      val duplicated: Seq[Rule[Foo]] = Rule.findDuplicated(
        NonEmptyList.of(
          Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
            Allow.withoutReasons
          },
          Rule("Check X value").checkOrIgnore[Foo] { case Foo("Foo", _) =>
            Allow.withoutReasons
          }
        )
      )

      duplicated shouldBe Nil
    }
  }
}
