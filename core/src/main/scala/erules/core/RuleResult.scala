package erules.core

import cats.{Eq, Order, Show}
import erules.core.RuleVerdict.Deny

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

case class RuleResult[-T, +V <: RuleVerdict](
  rule: AnyTypedRule[T],
  verdict: Try[V],
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
    RuleResult(Rule(ruleName).const[Try, T](v), Success(v))

  def failed[T, V <: RuleVerdict](ruleName: String, ex: Throwable): RuleResult[T, V] =
    RuleResult(Rule(ruleName).failed[Try, T](ex), Failure(ex))

  def noMatch[T, V <: RuleVerdict](v: V): RuleResult[T, V] =
    const("No match", v)

  def denyForSafetyInCaseOfError[T](rule: AnyTypedRule[T], ex: Throwable): RuleResult[T, Deny] =
    RuleResult(rule, Failure(ex))
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

  implicit def catsShowInstanceForRuleRuleResult[T]: Show[RuleResult[T, ? <: RuleVerdict]] =
    er => {

      val reasons: String = er.verdict.map(_.reasons) match {
        case Failure(ex)      => s"- Failed: $ex"
        case Success(Nil)     => ""
        case Success(reasons) => s"- Because: ${EvalReason.stringifyList(reasons)}"
      }

      s"""|- Rule: ${er.rule.name}
          |- Description: ${er.rule.description.getOrElse("")}
          |- Target: ${er.rule.targetInfo.getOrElse("")}
          |- Execution time: ${er.executionTime
           .map(Show.catsShowForFiniteDuration.show)
           .getOrElse("*not measured*")}
          |
          |- Verdict: ${er.verdict.map(_.typeName)}
          |$reasons""".stripMargin
    }
}
