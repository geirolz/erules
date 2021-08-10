package erules.core

import cats.{Monoid, Show}
import erules.core.utils.Summarizable
import erules.core.EvalRuleResult.{Allow, Deny, Ignore}

import scala.annotation.unused

/** ADT to define the possible output of a [[Rule]] evaluation.
  */
sealed trait EvalRuleResult extends Summarizable { this: EvalRuleResultBecauseSupport[EvalRuleResult] =>

  /** Result reasons
    */
  val reasons: List[EvalReason]

  /** Returns `true` if this is an instance of [[Allow]]
    */
  val isAllow: Boolean = this.isInstanceOf[Allow]

  /** Returns `true` if this is an instance of [[Deny]]
    */
  val isDeny: Boolean = this.isInstanceOf[Deny]

  /** Returns `true` if this is an instance of [[Ignore]]
    */
  val isIgnore: Boolean = this.isInstanceOf[Ignore]

  /** String that represent just the kind
    */
  val typeName: String = this match {
    case _: EvalRuleResult.Allow  => "Allow"
    case _: EvalRuleResult.Deny   => "Deny"
    case _: EvalRuleResult.Ignore => "Ignore"
  }

  override def summary: String = Show[EvalRuleResult].show(this)
}
private[erules] trait EvalRuleResultBecauseSupport[+T <: EvalRuleResult] {

  /** Append the specified reason
    */
  def because(newReason: String)(implicit @unused dummyImplicit: DummyImplicit): T =
    because(EvalReason(newReason))

  /** Append the specified reason
    */
  def because(newReason: EvalReason): T =
    because(List(newReason))

  /** Append the specified reasons
    */
  def because(newReasons: List[EvalReason]): T

  /** A [[EvalRuleResult]] without reasons
    */
  def withoutReasons: T
}

object EvalRuleResult {

  val noReasons: List[EvalReason] = Nil

  implicit val monoidInstanceForEvalRuleResult: Monoid[EvalRuleResult] = new Monoid[EvalRuleResult] {
    override def empty: EvalRuleResult = Ignore.withoutReasons
    override def combine(x: EvalRuleResult, y: EvalRuleResult): EvalRuleResult =
      (x, y) match {
        case (a: Allow, b: Allow)   => Allow.because(a.reasons ++ b.reasons)
        case (a: Deny, b: Deny)     => Deny.because(a.reasons ++ b.reasons)
        case (a: Ignore, b: Ignore) => Ignore.because(a.reasons ++ b.reasons)
        case (a: Deny, _)           => Deny.because(a.reasons)
        case (_, b: Deny)           => Deny.because(b.reasons)
        case (a: Allow, _)          => Allow.because(a.reasons)
        case (_, b: Allow)          => Allow.because(b.reasons)
      }
  }

  implicit val showInstanceForEvalRulResult: Show[EvalRuleResult] =
    e => s"${e.typeName}(${e.reasons})"

  //------------------------------ ALLOW ------------------------------
  sealed trait Allow extends EvalRuleResult with EvalRuleResultBecauseSupport[Allow]
  object Allow extends EvalRuleResultBecauseSupport[Allow] {

    val allNotExplicitlyDenied: Allow = Allow.because("Allow All Not Explicitly Denied")

    /** Create a [[Allow]] result with the specified reasons
      */
    override def because(newReasons: List[EvalReason]): Allow = AllowImpl(newReasons)

    /** Create a [[Allow]] result without reasons
      */
    override def withoutReasons: Allow = AllowImpl(noReasons)

    private case class AllowImpl(reasons: List[EvalReason]) extends Allow {

      /** Append the specified reasons
        */
      override def because(newReasons: List[EvalReason]): Allow =
        copy(reasons = reasons ++ newReasons)

      /** Remove all reasons
        */
      override def withoutReasons: Allow =
        copy(reasons = noReasons)
    }
  }

  //------------------------------ DENY ------------------------------
  sealed trait Deny extends EvalRuleResult with EvalRuleResultBecauseSupport[Deny]
  object Deny extends EvalRuleResultBecauseSupport[Deny] {

    val allNotExplicitlyAllowed: Deny = Deny.because("Deny All Not Explicitly Allowed")

    /** Create a [[Deny]] result with the specified reasons
      */
    override def because(newReasons: List[EvalReason]): Deny = DenyImpl(newReasons)

    /** Create a [[Deny]] result without reasons
      */
    override def withoutReasons: Deny = DenyImpl(noReasons)

    private case class DenyImpl(reasons: List[EvalReason]) extends Deny {

      /** Append the specified reasons
        */
      override def because(newReasons: List[EvalReason]): Deny =
        copy(reasons = reasons ++ newReasons)

      /** Remove all reasons
        */
      override def withoutReasons: Deny =
        copy(reasons = noReasons)
    }
  }

  //------------------------------ IGNORE ------------------------------
  sealed trait Ignore extends EvalRuleResult with EvalRuleResultBecauseSupport[Ignore]
  object Ignore extends EvalRuleResultBecauseSupport[Ignore] {

    val noMatch: Ignore = Ignore.because("No match")

    /** Create a [[Ignore]] result with the specified reasons
      */
    override def because(newReasons: List[EvalReason]): Ignore = IgnoreImpl(newReasons)

    /** Create a [[Ignore]] result without reasons
      */
    override def withoutReasons: Ignore = IgnoreImpl(noReasons)

    private case class IgnoreImpl(reasons: List[EvalReason]) extends Ignore {

      /** Append the specified reasons
        */
      override def because(newReasons: List[EvalReason]): Ignore =
        copy(reasons = reasons ++ newReasons)

      /** Remove all reasons
        */
      override def withoutReasons: Ignore =
        copy(reasons = noReasons)
    }
  }
}
