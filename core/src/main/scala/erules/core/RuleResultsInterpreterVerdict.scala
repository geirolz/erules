package erules.core

import cats.data.NonEmptyList
import erules.core.RuleResultsInterpreterVerdict.{Allowed, Denied}

/** ADT to define the possible responses of the engine evaluation.
  */
sealed trait RuleResultsInterpreterVerdict extends Serializable {

  /** Evaluated rules results
    */
  val evaluatedResults: NonEmptyList[RuleResult.Unbiased]

  /** Check if this is an instance of `Allowed` or not
    */
  val isAllowed: Boolean = this match {
    case Allowed(_) => true
    case Denied(_)  => false
  }

  /** Check if this is an instance of `Denied` or not
    */
  val isDenied: Boolean = !isAllowed

  /** Returns a string `Allowed` if this is an instance of `Allowed` otherwise `Denied`
    */
  val typeName: String = this match {
    case Allowed(_) => "Allowed"
    case Denied(_)  => "Denied"
  }
}
object RuleResultsInterpreterVerdict {

  case class Allowed(evaluatedResults: NonEmptyList[RuleResult[RuleVerdict.Allow]])
      extends RuleResultsInterpreterVerdict

  case class Denied(evaluatedResults: NonEmptyList[RuleResult[RuleVerdict.Deny]])
      extends RuleResultsInterpreterVerdict
}
