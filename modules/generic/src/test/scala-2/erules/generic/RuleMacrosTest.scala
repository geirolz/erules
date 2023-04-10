package erules.generic

import erules.core.{PureRule, Rule, RuleVerdict}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class RuleMacrosTest extends AnyFunSuite with Matchers {

  import erules.generic.implicits.*

  test("contramapTarget should contramap and add target info") {

    case class Foo(bar: Bar)
    case class Bar(test: Test)
    case class Test(value: Int)

    val rule: PureRule[Int]    = Rule("RULE").const(RuleVerdict.Ignore.withoutReasons)
    val fooRule: PureRule[Foo] = rule.contramapTarget[Foo](_.bar.test.value)

    fooRule.targetInfo shouldBe Some("bar.test.value")
  }

  test("contramapTarget should not compile with monadic values") {
    """
      import cats.Id
      
      case class Foo(b: Option[Bar])
      case class Bar(t: Option[Test])
      case class Test(value: Int)

      val rule: PureRule[Int] = Rule("RULE").const[Id, Int](RuleVerdict.Ignore.withoutReasons)
    
      rule.contramapTarget[Foo](_.b.flatMap(_.t.map(_.value)).get)
    """ shouldNot compile
  }
}
