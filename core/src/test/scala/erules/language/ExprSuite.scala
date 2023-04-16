package erules.language

import org.scalatest.funsuite.AnyFunSuite

class ExprSuite extends AnyFunSuite {

  import erules.language.Expr.*

  test("If") {
    val expr: If[String] =
      If("1" <= "1" && Equals("3", "3")) {
        If(IsEmpty(Seq(1, 2, 3))) {
          "IS EMPTY"
        } Else {
          "IS NON EMPTY"
        }
      } Else {
        "NOT EQUAL"
      }

    println(expr.toString)

  }

  test("Visualization") {
    val expr = (Equals("1", "1") && Equals("2", "2")) && IsEmpty(Seq(1, 2, 3))
    println(expr.toString)
  }

  test("Equals") {
    val expr: Equals[String] = Equals("1", "1")
    assert(expr.eval)
  }

  test("Not[Equals]") {
    val expr: Not[Equals[String]] = !!(Equals("1", "1"))
    assert(!expr.eval)
  }

  test("LessThen") {
    val expr: LessThen[Int] = LessThen(1, 2)
    assert(expr.eval)
  }

  test("LessThenEq") {
    val expr1: LessThenEq[Int] = LessThenEq(1, 2)
    val expr2: LessThenEq[Int] = LessThenEq(1, 1)
    assert(expr1.eval)
    assert(expr2.eval)
  }

  test("GreaterThan") {
    val expr: GreaterThan[Int] = GreaterThan(2, 1)
    assert(expr.eval)
  }

  test("GreaterThanEq") {
    val expr1: GreaterThanEq[Int] = GreaterThanEq(2, 1)
    val expr2: GreaterThanEq[Int] = GreaterThanEq(1, 1)
    assert(expr1.eval)
    assert(expr2.eval)
  }
}
