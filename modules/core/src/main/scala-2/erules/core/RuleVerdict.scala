package erules.core

import cats.{Monoid, Show}
import erules.core.RuleVerdict.{Allow, Deny, Ignore}

import scala.annotation.unused

/** ADT to define the possible output of a [[Rule]] evaluation.
  */
sealed trait RuleVerdict extends Serializable { this: RuleVerdictBecauseSupport[RuleVerdict] =>

  /** Result reasons
    */
  val reasons: List[EvalReason]

  /** Returns `true` if this is an instance of `Allow`
    */
  val isAllow: Boolean = this.isInstanceOf[Allow]

  /** Returns `true` if this is an instance of `Deny`
    */
  val isDeny: Boolean = this.isInstanceOf[Deny]

  /** Returns `true` if this is an instance of `Ignore`
    */
  val isIgnore: Boolean = this.isInstanceOf[Ignore]

  /** String that represent just the kind
    */
  val typeName: String = this match {
    case _: RuleVerdict.Allow  => "Allow"
    case _: RuleVerdict.Deny   => "Deny"
    case _: RuleVerdict.Ignore => "Ignore"
  }
}
private[erules] trait RuleVerdictBecauseSupport[+T <: RuleVerdict] {

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

  /** A [[RuleVerdict]] without reasons
    */
  def withoutReasons: T
}

object RuleVerdict extends RuleVerdictInstances {

  val noReasons: List[EvalReason] = Nil

  //------------------------------ ALLOW ------------------------------
  sealed trait Allow extends RuleVerdict with RuleVerdictBecauseSupport[Allow]
  object Allow extends RuleVerdictBecauseSupport[Allow] {

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
  sealed trait Deny extends RuleVerdict with RuleVerdictBecauseSupport[Deny]
  object Deny extends RuleVerdictBecauseSupport[Deny] {

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
  sealed trait Ignore extends RuleVerdict with RuleVerdictBecauseSupport[Ignore]
  object Ignore extends RuleVerdictBecauseSupport[Ignore] {

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

private[erules] trait RuleVerdictInstances {

  implicit val monoidInstanceForRuleVerdict: Monoid[RuleVerdict] = new Monoid[RuleVerdict] {
    override def empty: RuleVerdict = Ignore.withoutReasons
    override def combine(x: RuleVerdict, y: RuleVerdict): RuleVerdict =
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

  implicit val showInstanceForRuleVerdict: Show[RuleVerdict] =
    e => s"${e.typeName}(${e.reasons})"
}
