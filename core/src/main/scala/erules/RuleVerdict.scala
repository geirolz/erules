package erules

import cats.{Monoid, Show}
import erules.RuleVerdict.{Allow, Deny, Ignore}

import scala.annotation.unused

/** ADT to define the possible output of a [[Rule]] evaluation.
  */
sealed trait RuleVerdict extends Serializable with RuleVerdictBecauseSupport[RuleVerdict] {

  /** Result reasons
    */
  val reasons: List[EvalReason]

  /** Returns `true` if this is an instance of `Allow`
    */
  final val isAllow: Boolean = this.isInstanceOf[Allow]

  /** Returns `true` if this is an instance of `Deny`
    */
  final val isDeny: Boolean = this.isInstanceOf[Deny]

  /** Returns `true` if this is an instance of `Ignore`
    */
  final val isIgnore: Boolean = this.isInstanceOf[Ignore]

  /** String that represent just the kind
    */
  final val typeName: String = this match {
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

  // noinspection ScalaWeakerAccess
  lazy val noReasons: List[EvalReason] = Nil

  // TODO: add test
  def whenNot(b: Boolean)(ifTrue: => RuleVerdict, ifFalse: => RuleVerdict): RuleVerdict =
    when(!b)(ifFalse, ifTrue)

  // TODO: add test
  def when(b: Boolean)(ifTrue: => RuleVerdict, ifFalse: => RuleVerdict): RuleVerdict =
    if (b) ifTrue else ifFalse

  // ------------------------------ ALLOW ------------------------------
  sealed trait Allow extends RuleVerdict with RuleVerdictBecauseSupport[Allow]
  object Allow extends RuleVerdictBecauseSupport[Allow] {

    lazy val allNotExplicitlyDenied: Allow = Allow.because("Allow All Not Explicitly Denied")

    // TODO: add test
    def when(b: Boolean)(ifFalse: => RuleVerdict): RuleVerdict =
      RuleVerdict.when(b)(Allow.withoutReasons, ifFalse)

    // TODO: add test
    def whenNot(b: Boolean)(ifTrue: => RuleVerdict): RuleVerdict =
      RuleVerdict.whenNot(b)(ifTrue, Allow.withoutReasons)

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

  // ------------------------------ DENY ------------------------------
  sealed trait Deny extends RuleVerdict with RuleVerdictBecauseSupport[Deny]
  object Deny extends RuleVerdictBecauseSupport[Deny] {

    lazy val allNotExplicitlyAllowed: Deny = Deny.because("Deny All Not Explicitly Allowed")

    // TODO: add test
    def when(b: Boolean)(ifFalse: => RuleVerdict): RuleVerdict =
      RuleVerdict.when(b)(Deny.withoutReasons, ifFalse)

    // TODO: add test
    def whenNot(b: Boolean)(ifTrue: => RuleVerdict): RuleVerdict =
      RuleVerdict.whenNot(b)(ifTrue, Deny.withoutReasons)

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

  // ------------------------------ IGNORE ------------------------------
  sealed trait Ignore extends RuleVerdict with RuleVerdictBecauseSupport[Ignore]
  object Ignore extends RuleVerdictBecauseSupport[Ignore] {

    lazy val noMatch: Ignore = Ignore.because("No match")

    // TODO: add test
    def when(b: Boolean)(ifFalse: => RuleVerdict): RuleVerdict =
      RuleVerdict.when(b)(Ignore.withoutReasons, ifFalse)

    // TODO: add test
    def whenNot(b: Boolean)(ifTrue: => RuleVerdict): RuleVerdict =
      RuleVerdict.whenNot(b)(ifTrue, Ignore.withoutReasons)

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
