package erules.core

import cats.Show
import erules.core.RuleEvalResult.Ignore

import scala.annotation.unused

sealed trait Rule[-T] {
  val description: String
  def eval(data: T): RuleEvalResult
  final def evalZip(data: T): Rule.Evaluated[T] = Rule.TypedEvaluated[T, RuleEvalResult](this, eval(data))
  final override def toString: String = Show[Rule[T]].show(this)
}
object Rule extends RuleInstances {

  val noMatch: Rule[Any] = Rule("No match").const(Ignore.noMatch)

  //=================/ OPS /=================
  def apply[T](description: String): RuleBuilder[T] = new RuleBuilder[T](description)

  class RuleBuilder[T] private[Rule] (description: String) {

    def apply(f: T => RuleEvalResult): Rule[T] =
      RuleImpl(description, f)

    def apply(f: PartialFunction[T, RuleEvalResult])(implicit @unused dum: DummyImplicit): Rule[T] =
      RuleImpl(description, f.lift(_).getOrElse(Ignore.noMatch))

    def const(v: RuleEvalResult): Rule[T] =
      RuleImpl(description, _ => v)
  }

  private case class RuleImpl[-T](description: String, f: T => RuleEvalResult) extends Rule[T] {
    override def eval(data: T): RuleEvalResult = f(data)
  }

  //=================/ ADT /=================
  type Evaluated[-T] = TypedEvaluated[T, RuleEvalResult]
  case class TypedEvaluated[-T, +R <: RuleEvalResult](rule: Rule[T], result: R) {
    override def toString: String = Show[TypedEvaluated[T, R]].show(this)
  }
  object TypedEvaluated {
    def noMatch[R <: RuleEvalResult](v: R): TypedEvaluated[Any, R] = TypedEvaluated(Rule.noMatch, v)
  }
}

private[erules] trait RuleInstances {

  implicit def showInstanceForRule[T]: Show[Rule[T]] =
    r => s"Rule('${r.description}')"

  implicit def showInstanceForRuleTypedEvaluated[T, R <: RuleEvalResult](implicit
    showRule: Show[Rule[T]]
  ): Show[Rule.TypedEvaluated[T, R]] =
    r =>
      s""""
            |Rule: ${showRule.show(r.rule)}
            |Result: ${r.result}
            |${r.result.reasons match {
        case Nil     => ""
        case reasons => s"Because:\n${reasons.map(r => s"-${r.message}").mkString("\n")}"
      }}
            |"""".stripMargin
}
