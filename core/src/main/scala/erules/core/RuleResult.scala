package erules.core

import cats.{Eq, Order}
import erules.core.RuleVerdict.Deny

import scala.concurrent.duration.FiniteDuration

case class RuleResult[-T, +V <: RuleVerdict](
  rule: AnyTypedRule[T],
  verdict: EitherThrow[V],
  executionTime: Option[FiniteDuration] = None
) extends Serializable {

  def mapRule[TT <: T](f: AnyTypedRule[TT] => AnyTypedRule[TT]): RuleResult[TT, V] =
    copy(rule = f(rule))

  def drainExecutionTime: RuleResult[T, V] =
    copy(executionTime = None)
}
object RuleResult extends RuleResultInstances {

  type Free[-T] = RuleResult[T, RuleVerdict]

  def const[T, V <: RuleVerdict](ruleName: String, v: V): RuleResult[T, V] =
    RuleResult(Rule(ruleName).const[EitherThrow, T](v), Right(v))

  def failed[T, V <: RuleVerdict](ruleName: String, ex: Throwable): RuleResult[T, V] =
    RuleResult(Rule(ruleName).failed[EitherThrow, T](ex), Left(ex))

  def noMatch[T, V <: RuleVerdict](v: V): RuleResult[T, V] =
    const("No match", v)

  def denyForSafetyInCaseOfError[T](rule: AnyTypedRule[T], ex: Throwable): RuleResult[T, Deny] =
    RuleResult(rule, Left(ex))
}

private[erules] trait RuleResultInstances {

  implicit def catsOrderInstanceForRuleRuleResult[T, V <: RuleVerdict](implicit
    ruleEq: Eq[AnyTypedRule[T]]
  ): Order[RuleResult[T, V]] =
    Order.from((x, y) =>
      if (
        x != null
        && y != null
        && ruleEq.eqv(x.rule, y.rule)
        && x.verdict.equals(y.verdict)
        && x.executionTime.equals(y.executionTime)
      ) 0
      else -1
    )
}
