package erules

import cats.Order
import erules.RuleVerdict.*

import scala.concurrent.duration.FiniteDuration

case class RuleResult[+V <: RuleVerdict](
  ruleInfo: RuleInfo,
  verdict: EitherThrow[V],
  executionTime: Option[FiniteDuration]
) extends Serializable {

  def drainExecutionTime: RuleResult[V] =
    copy(executionTime = None)
}
object RuleResult extends RuleResultInstances {

  type Unbiased = RuleResult[RuleVerdict]

  private[RuleResult] def apply[V <: RuleVerdict](
    ruleInfo: RuleInfo,
    verdict: EitherThrow[V],
    executionTime: Option[FiniteDuration]
  ): RuleResult[V] = new RuleResult(ruleInfo, verdict, executionTime)

  def apply(ruleInfo: RuleInfo): RuleResultBuilder =
    new RuleResultBuilder(ruleInfo)

  def forRuleName(ruleName: String): RuleResultBuilder =
    apply(RuleInfo(ruleName))

  def forRule[F[_], T](rule: Rule[F, T]): RuleResultBuilder =
    apply(rule.info)

  def noMatch[V <: RuleVerdict](v: V): RuleResult[V] =
    forRuleName("No match").succeeded(v)

  class RuleResultBuilder(private val ruleInfo: RuleInfo) {

    def apply[V <: RuleVerdict](
      verdict: EitherThrow[V],
      executionTime: Option[FiniteDuration] = None
    ): RuleResult[V] =
      RuleResult(ruleInfo, verdict, executionTime)

    def succeeded[V <: RuleVerdict](
      v: V,
      executionTime: Option[FiniteDuration] = None
    ): RuleResult[V] =
      apply(Right(v), executionTime)

    def failed[V <: RuleVerdict](
      ex: Throwable,
      executionTime: Option[FiniteDuration] = None
    ): RuleResult[V] =
      apply(Left(ex), executionTime)

    def allow(executionTime: Option[FiniteDuration] = None): RuleResult[Allow] =
      succeeded(Allow.withoutReasons, executionTime)

    def denyForSafetyInCaseOfError(
      ex: Throwable,
      executionTime: Option[FiniteDuration] = None
    ): RuleResult[Deny] =
      apply(Left(ex), executionTime)
  }
}

private[erules] trait RuleResultInstances {

  implicit def catsOrderInstanceForRuleRuleResult[V <: RuleVerdict]: Order[RuleResult[V]] =
    Order.from((x, y) =>
      if (
        x != null
        && y != null
        && x.ruleInfo.equals(y.ruleInfo)
        && x.verdict.equals(y.verdict)
        && x.executionTime.equals(y.executionTime)
      ) 0
      else -1
    )
}
