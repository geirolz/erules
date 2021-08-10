package erules.core

import scala.annotation.unused

/** ADT to define the possible output of a [[Rule]] evaluation.
  */
sealed trait RuleEvalResult { this: EvalResultBecauseSupport[RuleEvalResult] =>

  /** Result reasons
    */
  val reasons: List[EvalResultReason]
}
private[core] trait EvalResultBecauseSupport[+T <: RuleEvalResult] {

  /** Append the specified reason
    */
  def because(newReason: String)(implicit @unused dummyImplicit: DummyImplicit): T =
    because(EvalResultReason(newReason))

  /** Append the specified reason
    */
  def because(newReason: EvalResultReason): T =
    because(List(newReason))

  /** Append the specified reasons
    */
  def because(newReasons: List[EvalResultReason]): T

  /** A [[RuleEvalResult]] without reasons
    */
  def withoutReasons: T
}

object RuleEvalResult {

  val noReasons: List[EvalResultReason] = Nil

  //------------------------------ ALLOW ------------------------------
  sealed trait Allow extends RuleEvalResult with EvalResultBecauseSupport[Allow]
  object Allow extends EvalResultBecauseSupport[Allow] {

    /** Create a [[Allow]] result with the specified reasons
      */
    override def because(newReasons: List[EvalResultReason]): Allow = AllowImpl(newReasons)

    /**  Create a [[Allow]] result without reasons
      */
    override def withoutReasons: Allow = AllowImpl(noReasons)

    private case class AllowImpl(reasons: List[EvalResultReason]) extends Allow {

      /** Append the specified reasons
        */
      override def because(newReasons: List[EvalResultReason]): Allow =
        copy(reasons = reasons ++ newReasons)

      /** Remove all reasons
        */
      override def withoutReasons: Allow =
        copy(reasons = noReasons)
    }
  }

  //------------------------------ DENY ------------------------------
  sealed trait Deny extends RuleEvalResult with EvalResultBecauseSupport[Deny]
  object Deny extends EvalResultBecauseSupport[Deny] {

    /** Create a [[Deny]] result with the specified reasons
      */
    override def because(newReasons: List[EvalResultReason]): Deny = DenyImpl(newReasons)

    /**  Create a [[Deny]] result without reasons
      */
    override def withoutReasons: Deny = DenyImpl(noReasons)

    private case class DenyImpl(reasons: List[EvalResultReason]) extends Deny {

      /** Append the specified reasons
        */
      override def because(newReasons: List[EvalResultReason]): Deny =
        copy(reasons = reasons ++ newReasons)

      /** Remove all reasons
        */
      override def withoutReasons: Deny =
        copy(reasons = noReasons)
    }
  }

  //------------------------------ IGNORE ------------------------------
  sealed trait Ignore extends RuleEvalResult with EvalResultBecauseSupport[Ignore]
  object Ignore extends EvalResultBecauseSupport[Ignore] {

    val noMatch: Ignore = Ignore.because("No match")

    /** Create a [[Ignore]] result with the specified reasons
      */
    override def because(newReasons: List[EvalResultReason]): Ignore = IgnoreImpl(newReasons)

    /**  Create a [[Ignore]] result without reasons
      */
    override def withoutReasons: Ignore = IgnoreImpl(noReasons)

    private case class IgnoreImpl(reasons: List[EvalResultReason]) extends Ignore {

      /** Append the specified reasons
        */
      override def because(newReasons: List[EvalResultReason]): Ignore =
        copy(reasons = reasons ++ newReasons)

      /** Remove all reasons
        */
      override def withoutReasons: Ignore =
        copy(reasons = noReasons)
    }
  }
}
