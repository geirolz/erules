package erules.core

import cats.data.NonEmptyList
import cats.Show
import erules.core.EvalRulesInterpreterResult.{Allowed, Denied}
import erules.core.utils.Summarizable

/** ADT to define the possible output of the engine evaluation.
  */
sealed trait EvalRulesInterpreterResult[T] extends Summarizable {

  /** Result reasons
    */
  val evaluatedRules: NonEmptyList[Rule.Evaluated[T]]

  /** Check if this is an instance of `Allowed` or not
    */
  val isAllowed: Boolean = this match {
    case Allowed(_) => true
    case Denied(_)  => false
  }

  /** Check if this is an instance of `Denied` or not
    */
  val isDenied: Boolean = !isAllowed

  /** Returns a string `Allowed` if this is an instance of [[Allowed]] otherwise `Denied`
    */
  val typeName: String = this match {
    case Allowed(_) => "Allowed"
    case Denied(_)  => "Denied"
  }

  override def summary: String = Show[EvalRulesInterpreterResult[T]].show(this)
}
object EvalRulesInterpreterResult {

  case class Allowed[T](evaluatedRules: NonEmptyList[Rule.TypedEvaluated[T, EvalRuleResult.Allow]])
      extends EvalRulesInterpreterResult[T]

  case class Denied[T](evaluatedRules: NonEmptyList[Rule.TypedEvaluated[T, EvalRuleResult.Deny]])
      extends EvalRulesInterpreterResult[T]

  implicit def catsShowInstanceForEvalRulesInterpreterResult[T](implicit
    evalRuleShow: Show[Rule.TypedEvaluated[T, ? <: EvalRuleResult]]
  ): Show[EvalRulesInterpreterResult[T]] =
    t => {

      val rulesReport: String = t.evaluatedRules
        .map(er =>
          Summarizable.paragraph(er.rule.fullDescription)(
            evalRuleShow.show(er)
          )
        )
        .toList
        .mkString("\n")

      val tpe: String = t match {
        case Allowed(_) => "Allowed"
        case Denied(_)  => "Denied"
      }

      s"""InterpreterResult: $tpe
        |
        |$rulesReport
        |""".stripMargin
    }
}
