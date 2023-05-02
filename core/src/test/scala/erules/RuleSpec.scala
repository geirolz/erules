package erules

import cats.data.NonEmptyList
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO
import erules.RuleVerdict.{Allow, Deny, Ignore}
import erules.testings.{ErulesAsyncAssertingSyntax, ReportValues}
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers

import scala.annotation.unused

class RuleSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers
    with EitherValues
    with ErulesAsyncAssertingSyntax
    with ReportValues {

  // ------------------------- CONTRAMAP -------------------------
  "Rule.contramap" should {
    "Apply the contravariant map widening the type" in {

      case class Foo(bar: Bar)
      case class Bar(baz: Baz)
      case class Baz(value: String)

      val bazRule: PureRule[Baz] = Rule("Check Baz value")(
        _.value match {
          case "" => Deny.because("Empty value")
          case _  => Allow.withoutReasons
        }
      )

      val fooRule: PureRule[Foo] = bazRule.contramap(_.bar.baz)

      bazRule.evalRaw(Baz("")) shouldBe Deny.because("Empty value")
      bazRule.evalRaw(Baz("baz")) shouldBe Allow.withoutReasons
      fooRule.evalRaw(Foo(Bar(Baz("")))) shouldBe Deny.because("Empty value")
      fooRule.evalRaw(Foo(Bar(Baz("bar")))) shouldBe Allow.withoutReasons
    }
  }

  // ------------------------- EVAL -------------------------
  "Rule.apply.eval" should {
    "return the right result once evaluated" in {

      sealed trait ADT
      case class Foo(@unused x: String, @unused y: Int) extends ADT
      case class Bar(@unused x: String, @unused y: Int) extends ADT

      val rule: RuleIO[ADT] = Rule("Check Y value") {
        case Foo(_, 0) => IO.pure(Allow.withoutReasons)
        case Bar(_, 1) => IO.pure(Deny.withoutReasons)
        case _         => IO.pure(Ignore.withoutReasons)
      }

      for {
        _ <- rule
          .eval(Foo("TEST", 0))
          .assertingIgnoringTimes(
            _ shouldBe RuleResult.forRule(rule).succeeded(Allow.withoutReasons)
          )
        _ <- rule
          .eval(Bar("TEST", 1))
          .assertingIgnoringTimes(
            _ shouldBe RuleResult.forRule(rule).succeeded(Deny.withoutReasons)
          )
      } yield ()
    }

    "return an exception when a case fail" in {

      sealed trait ADT
      case class Foo(@unused x: String, @unused y: Int) extends ADT
      case class Bar(@unused x: String, @unused y: Int) extends ADT

      val ex = new RuntimeException("BOOM")

      val rule: RuleIO[ADT] = Rule("Check Y value") {
        case Foo(_, 0) => IO.raiseError(ex)
        case Bar(_, 1) => IO.pure(Deny.withoutReasons)
        case _         => IO.pure(Ignore.withoutReasons)

      }

      for {
        _ <- rule
          .eval(Foo("TEST", 0))
          .assertingIgnoringTimes(_ shouldBe RuleResult.forRule(rule).failed(ex))
        _ <- rule
          .eval(Bar("TEST", 1))
          .assertingIgnoringTimes(
            _ shouldBe RuleResult.forRule(rule).succeeded(Deny.withoutReasons)
          )
      } yield ()
    }

    "return the right result once evaluated when exhaustive" in {
      sealed trait ADT
      case class Foo() extends ADT
      case class Bar() extends ADT

      val rule: PureRule[ADT] = Rule("Check Y value") {
        case Foo() => Allow.withoutReasons
        case Bar() => Deny.withoutReasons
      }

      for {
        _ <- rule
          .liftK[IO]
          .eval(Foo())
          .assertingIgnoringTimes(
            _ shouldBe RuleResult.forRule(rule).succeeded(Allow.withoutReasons)
          )
        _ <- rule
          .liftK[IO]
          .eval(Bar())
          .assertingIgnoringTimes(
            _ shouldBe RuleResult.forRule(rule).succeeded(Deny.withoutReasons)
          )
      } yield ()
    }

  }

  "Rule.partially.eval" should {
    "return the right result once evaluated" in {
      case class Foo(@unused x: String, @unused y: Int)

      val rule: RuleIO[Foo] = Rule("Check Y value").partially {
        case Foo(_, 0) => IO.pure(Allow.withoutReasons)
        case Foo(_, 1) => IO.pure(Deny.withoutReasons)
      }

      for {
        _ <- rule
          .eval(Foo("TEST", 0))
          .assertingIgnoringTimes(
            _ shouldBe RuleResult.forRule(rule).succeeded(Allow.withoutReasons)
          )
        _ <- rule
          .eval(Foo("TEST", 1))
          .assertingIgnoringTimes(
            _ shouldBe RuleResult.forRule(rule).succeeded(Deny.withoutReasons)
          )
      } yield ()
    }

    "return an exception when a case fail" in {
      case class Foo(@unused x: String, @unused y: Int)
      val ex = new RuntimeException("BOOM")

      val rule: RuleIO[Foo] = Rule("Check Y value").partially {
        case Foo(_, 0) => IO.raiseError(ex)
        case Foo(_, 1) => IO.pure(Deny.withoutReasons)
      }

      for {
        _ <- rule
          .eval(Foo("TEST", 0))
          .assertingIgnoringTimes(_ shouldBe RuleResult.forRule(rule).failed(ex))
        _ <- rule
          .eval(Foo("TEST", 1))
          .assertingIgnoringTimes(
            _ shouldBe RuleResult.forRule(rule).succeeded(Deny.withoutReasons)
          )
      } yield ()
    }

    "return the right result once evaluated in defined domain" in {
      case class Foo(@unused x: String, @unused y: Int)

      val rule: PureRule[Foo] = Rule("Check Y value").partially { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      rule
        .liftK[IO]
        .eval(Foo("TEST", 0))
        .assertingIgnoringTimes(_ shouldBe RuleResult.forRule(rule).succeeded(Allow.withoutReasons))
    }

    "return the Ignore once evaluated out of the defined domain" in {
      case class Foo(@unused x: String, @unused y: Int)

      val rule: PureRule[Foo] = Rule("Check Y value").partially { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      rule
        .liftK[IO]
        .eval(Foo("TEST", 1))
        .assertingIgnoringTimes(
          _ shouldBe RuleResult.forRule(rule).succeeded(Ignore.noMatch)
        )
    }
  }

  "Rule.assert*" should {

    "assert" in {
      Rule
        .pure[String]("Non Empty String")
        .assert("String must be non empty")(_.nonEmpty)
        .evalPure("NON_EMPTY_STRING")
        .verdict shouldBe Right(Allow.withoutReasons)

      Rule
        .pure[String]("Non Empty String")
        .assert("String must be non empty")(_.nonEmpty)
        .evalPure("")
        .verdict shouldBe Right(Deny.because("String must be non empty"))

      Rule
        .pure[String]("Non Empty String")
        .assert(_.nonEmpty)
        .evalPure("")
        .verdict shouldBe Right(Deny.withoutReasons)
    }

    "assertNot" in {
      Rule
        .pure[String]("Non Empty String")
        .assertNot("String must be non empty")(_.isEmpty)
        .evalPure("NON_EMPTY_STRING")
        .verdict shouldBe Right(Allow.withoutReasons)

      Rule
        .pure[String]("Non Empty String")
        .assertNot("String must be non empty")(_.isEmpty)
        .evalPure("")
        .verdict shouldBe Right(Deny.because("String must be non empty"))

      Rule
        .pure[String]("Non Empty String")
        .assertNot(_.isEmpty)
        .evalPure("")
        .verdict shouldBe Right(Deny.withoutReasons)
    }
  }
  // ------------------------- EVAL RAW -------------------------
  "Rule.apply.evalRaw" should {
    "return the right result once evaluated" in {

      sealed trait ADT
      case class Foo(@unused x: String, @unused y: Int) extends ADT
      case class Bar(@unused x: String, @unused y: Int) extends ADT

      val rule: Rule[IO, ADT] = Rule("Check Y value") {
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
      case class Foo(@unused x: String, @unused y: Int) extends ADT
      case class Bar(@unused x: String, @unused y: Int) extends ADT

      val rule: RuleIO[ADT] = Rule("Check Y value") {
        case Foo(_, 0) => IO.raiseError(new RuntimeException("BOOM"))
        case Bar(_, 1) => IO.pure(Deny.withoutReasons)
        case _         => IO.pure(Ignore.withoutReasons)
      }

      for {
        _ <- rule.evalRaw(Foo("TEST", 0)).assertThrows[RuntimeException]
        _ <- rule.evalRaw(Bar("TEST", 1)).asserting(_ shouldBe RuleVerdict.Deny.withoutReasons)
      } yield ()
    }

    "return the right result once evaluated when exhaustive" in {
      sealed trait ADT
      case class Foo() extends ADT
      case class Bar() extends ADT

      val rule: PureRule[ADT] = Rule("Check Y value") {
        case Foo() => Allow.withoutReasons
        case Bar() => Deny.withoutReasons
      }

      rule.evalRaw(Foo()) shouldBe RuleVerdict.Allow.withoutReasons
      rule.evalRaw(Bar()) shouldBe RuleVerdict.Deny.withoutReasons
    }
  }

  "Rule.partially.evalRaw" should {
    "return the right result once evaluated" in {
      case class Foo(@unused x: String, @unused y: Int)

      val rule: Rule[IO, Foo] = Rule("Check Y value").partially {
        case Foo(_, 0) => IO.pure(Allow.withoutReasons)
        case Foo(_, 1) => IO.pure(Deny.withoutReasons)
      }

      for {
        _ <- rule.evalRaw(Foo("TEST", 0)).asserting(_ shouldBe RuleVerdict.Allow.withoutReasons)
        _ <- rule.evalRaw(Foo("TEST", 1)).asserting(_ shouldBe RuleVerdict.Deny.withoutReasons)
      } yield ()
    }

    "return an exception when a case fail" in {
      case class Foo(@unused x: String, @unused y: Int)

      val rule: RuleIO[Foo] = Rule("Check Y value").partially {
        case Foo(_, 0) => IO.raiseError(new RuntimeException("BOOM"))
        case Foo(_, 1) => IO.pure(Deny.withoutReasons)
      }

      for {
        _ <- rule.evalRaw(Foo("TEST", 0)).assertThrows[RuntimeException]
        _ <- rule.evalRaw(Foo("TEST", 1)).asserting(_ shouldBe RuleVerdict.Deny.withoutReasons)
      } yield ()
    }

    "return the right result once evaluated in defined domain" in {
      case class Foo(@unused x: String, @unused y: Int)

      val rule: PureRule[Foo] =
        Rule("Check Y value").partially { case Foo(_, 0) =>
          Allow.withoutReasons
        }

      rule.evalRaw(Foo("TEST", 0)) shouldBe RuleVerdict.Allow.withoutReasons
    }

    "return the Ignore once evaluated out of the defined domain" in {
      case class Foo(@unused x: String, @unused y: Int)

      val rule: PureRule[Foo] =
        Rule("Check Y value").partially { case Foo(_, 0) =>
          Allow.withoutReasons
        }

      rule.evalRaw(Foo("TEST", 1)) shouldBe RuleVerdict.Ignore.noMatch
    }
  }

  // ------------------------- UTILS -------------------------
  "Rule.findDuplicated" should {
    "return the list of duplicated rules" in {
      case class Foo(@unused x: String, @unused y: Int)

      val duplicated: List[PureRule[Foo]] = Rule.findDuplicated(
        NonEmptyList.of(
          Rule("Check Y value").partially { case Foo(_, 0) =>
            Allow.withoutReasons
          },
          Rule("Check Y value").partially { case Foo(_, 1) =>
            Allow.withoutReasons
          }
        )
      )

      duplicated.map(_.name) shouldBe List("Check Y value")
    }

    "return a Nil when there are no duplicated descriptions" in {
      case class Foo(@unused x: String, @unused y: Int)

      val duplicated: Seq[PureRule[Foo]] = Rule.findDuplicated(
        NonEmptyList.of(
          Rule("Check Y value").partially { case Foo(_, 0) =>
            Allow.withoutReasons
          },
          Rule("Check X value").partially { case Foo("Foo", _) =>
            Allow.withoutReasons
          }
        )
      )

      duplicated shouldBe Nil
    }
  }
}
