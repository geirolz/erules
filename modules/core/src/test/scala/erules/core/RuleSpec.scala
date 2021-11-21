package erules.core

import cats.data.NonEmptyList
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO
import erules.core.RuleVerdict.{Allow, Deny, Ignore}
import erules.core.testings.{ErulesAsyncAssertingSyntax, ReportValues}
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.TryValues
import org.scalatest.matchers.should.Matchers

import scala.util.{Failure, Success}

class RuleSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers
    with TryValues
    with ErulesAsyncAssertingSyntax
    with ReportValues {

  // ------------------------- CONTRAMAP -------------------------
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

      for {
        _ <- bazRule.evalRaw(Baz("")).asserting(_ shouldBe Deny.because("Empty value"))
        _ <- bazRule.evalRaw(Baz("baz")).asserting(_ shouldBe Allow.withoutReasons)
        _ <- fooRule.evalRaw(Foo(Bar(Baz("")))).asserting(_ shouldBe Deny.because("Empty value"))
        _ <- fooRule.evalRaw(Foo(Bar(Baz("bar")))).asserting(_ shouldBe Allow.withoutReasons)
      } yield ()
    }
  }

  // ------------------------- EVAL ZIP -------------------------
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

      for {
        _ <- rule
          .eval(Foo("TEST", 0))
          .assertingIgnoringTimes(_ shouldBe RuleResult(rule, Success(Allow.withoutReasons)))
        _ <- rule
          .eval(Bar("TEST", 1))
          .assertingIgnoringTimes(_ shouldBe RuleResult(rule, Success(Deny.withoutReasons)))
      } yield ()
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

      for {
        _ <- rule
          .eval(Foo("TEST", 0))
          .assertingIgnoringTimes(_ shouldBe RuleResult(rule, Failure(ex)))
        _ <- rule
          .eval(Bar("TEST", 1))
          .assertingIgnoringTimes(_ shouldBe RuleResult(rule, Success(Deny.withoutReasons)))
      } yield ()
    }
  }

  "Rule.asyncCheckOrIgnore.eval" should {
    "return the right result once evaluated" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").asyncCheckOrIgnore[Foo] {
        case Foo(_, 0) => IO.pure(Allow.withoutReasons)
        case Foo(_, 1) => IO.pure(Deny.withoutReasons)
      }

      for {
        _ <- rule
          .eval(Foo("TEST", 0))
          .assertingIgnoringTimes(_ shouldBe RuleResult(rule, Success(Allow.withoutReasons)))
        _ <- rule
          .eval(Foo("TEST", 1))
          .assertingIgnoringTimes(_ shouldBe RuleResult(rule, Success(Deny.withoutReasons)))
      } yield ()
    }

    "return an exception when a case fail" in {
      case class Foo(x: String, y: Int)
      val ex = new RuntimeException("BOOM")

      val rule: Rule[Foo] = Rule("Check Y value").asyncCheckOrIgnore[Foo] {
        case Foo(_, 0) => IO.raiseError(ex)
        case Foo(_, 1) => IO.pure(Deny.withoutReasons)
      }

      for {
        _ <- rule
          .eval(Foo("TEST", 0))
          .assertingIgnoringTimes(_ shouldBe RuleResult(rule, Failure(ex)))
        _ <- rule
          .eval(Foo("TEST", 1))
          .assertingIgnoringTimes(_ shouldBe RuleResult(rule, Success(Deny.withoutReasons)))
      } yield ()
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

      for {
        _ <- rule
          .eval(Foo())
          .assertingIgnoringTimes(_ shouldBe RuleResult(rule, Success(Allow.withoutReasons)))
        _ <- rule
          .eval(Bar())
          .assertingIgnoringTimes(_ shouldBe RuleResult(rule, Success(Deny.withoutReasons)))
      } yield ()
    }
  }

  "Rule.checkOrIgnore.eval" should {
    "return the right result once evaluated in defined domain" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      rule
        .eval(Foo("TEST", 0))
        .assertingIgnoringTimes(_ shouldBe RuleResult(rule, Success(Allow.withoutReasons)))
    }

    "return the Ignore once evaluated out of the defined domain" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      rule
        .eval(Foo("TEST", 1))
        .assertingIgnoringTimes(_ shouldBe RuleResult(rule, Success(Ignore.noMatch)))
    }
  }

  // ------------------------- EVAL RAW -------------------------
  "Rule.asyncCheck.evalRaw" should {
    "return the right result once evaluated" in {

      sealed trait ADT
      case class Foo(x: String, y: Int) extends ADT
      case class Bar(x: String, y: Int) extends ADT

      val rule: Rule[ADT] = Rule("Check Y value").asyncCheck[ADT] {
        case Foo(_, 0) => IO.pure(Allow.withoutReasons)
        case Bar(_, 1) => IO.pure(Deny.withoutReasons)
        case _         => IO.pure(Ignore.withoutReasons)
      }

      for {
        _ <- rule.evalRaw(Foo("TEST", 0)).asserting(_ shouldBe RuleVerdict.Allow.withoutReasons)
        _ <- rule.evalRaw(Bar("TEST", 1)).asserting(_ shouldBe RuleVerdict.Deny.withoutReasons)
      } yield ()
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

      for {
        _ <- rule.evalRaw(Foo("TEST", 0)).assertThrows[RuntimeException]
        _ <- rule.evalRaw(Bar("TEST", 1)).asserting(_ shouldBe RuleVerdict.Deny.withoutReasons)
      } yield ()
    }
  }

  "Rule.asyncCheckOrIgnore.evalRaw" should {
    "return the right result once evaluated" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").asyncCheckOrIgnore[Foo] {
        case Foo(_, 0) => IO.pure(Allow.withoutReasons)
        case Foo(_, 1) => IO.pure(Deny.withoutReasons)
      }

      for {
        _ <- rule.evalRaw(Foo("TEST", 0)).asserting(_ shouldBe RuleVerdict.Allow.withoutReasons)
        _ <- rule.evalRaw(Foo("TEST", 1)).asserting(_ shouldBe RuleVerdict.Deny.withoutReasons)
      } yield ()
    }

    "return an exception when a case fail" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").asyncCheckOrIgnore[Foo] {
        case Foo(_, 0) => IO.raiseError(new RuntimeException("BOOM"))
        case Foo(_, 1) => IO.pure(Deny.withoutReasons)
      }

      for {
        _ <- rule.evalRaw(Foo("TEST", 0)).assertThrows[RuntimeException]
        _ <- rule.evalRaw(Foo("TEST", 1)).asserting(_ shouldBe RuleVerdict.Deny.withoutReasons)
      } yield ()
    }
  }

  "Rule.check.evalRaw" should {
    "return the right result once evaluated when exhaustive" in {
      sealed trait ADT
      case class Foo() extends ADT
      case class Bar() extends ADT

      val rule: Rule[ADT] = Rule("Check Y value").check[ADT] {
        case Foo() => Allow.withoutReasons
        case Bar() => Deny.withoutReasons
      }

      for {
        _ <- rule.evalRaw(Foo()).asserting(_ shouldBe RuleVerdict.Allow.withoutReasons)
        _ <- rule.evalRaw(Bar()).asserting(_ shouldBe RuleVerdict.Deny.withoutReasons)
      } yield ()
    }
  }

  "Rule.checkOrIgnore.evalRaw" should {
    "return the right result once evaluated in defined domain" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      rule.evalRaw(Foo("TEST", 0)).asserting(_ shouldBe RuleVerdict.Allow.withoutReasons)
    }

    "return the Ignore once evaluated out of the defined domain" in {
      case class Foo(x: String, y: Int)

      val rule: Rule[Foo] = Rule("Check Y value").checkOrIgnore[Foo] { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      rule.evalRaw(Foo("TEST", 1)).asserting(_ shouldBe RuleVerdict.Ignore.noMatch)
    }
  }

  // ------------------------- UTILS -------------------------
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
