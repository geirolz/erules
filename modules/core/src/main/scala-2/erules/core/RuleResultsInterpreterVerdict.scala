package erules.core

import cats.data.NonEmptyList
import cats.Show
import erules.core.RuleResultsInterpreterVerdict.{Allowed, Denied}
import erules.core.report.StringReport

/** ADT to define the possible responses of the engine evaluation.
  */
sealed trait RuleResultsInterpreterVerdict[-T] extends Serializable {

  /** Result reasons
    */
  val evaluatedRules: NonEmptyList[RuleResult.Free[T]]

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
}
object RuleResultsInterpreterVerdict {

  case class Allowed[T](evaluatedRules: NonEmptyList[RuleResult[T, RuleVerdict.Allow]])
      extends RuleResultsInterpreterVerdict[T]

  case class Denied[T](evaluatedRules: NonEmptyList[RuleResult[T, RuleVerdict.Deny]])
      extends RuleResultsInterpreterVerdict[T]

  implicit def catsShowInstanceForRuleResultsInterpreterVerdict[T](implicit
    evalRuleShow: Show[RuleResult[T, ? <: RuleVerdict]]
  ): Show[RuleResultsInterpreterVerdict[T]] =
    t => {

      val rulesReport: String = t.evaluatedRules
        .map(er =>
          StringReport.paragraph(er.rule.fullDescription)(
            evalRuleShow.show(er)
          )
        )
        .toList
        .mkString("\n")

      val tpe: String = t match {
        case Allowed(_) => "Allowed"
        case Denied(_)  => "Denied"
      }

      s"""Interpreter verdict: $tpe
         |
         |$rulesReport
         |""".stripMargin
    }
}
