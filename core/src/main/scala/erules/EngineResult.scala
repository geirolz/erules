package erules

import erules.RuleResultsInterpreterVerdict.{Allowed, Denied}

/** Describes the engine output.
  *
  * @param data
  *   Data used for the evaluation
  * @param verdict
  *   Final engine verdict, the decision taken by then interpreter using the report.
  * @tparam T
  *   Data type
  */
case class EngineResult[T](
  data: T,
  verdict: RuleResultsInterpreterVerdict
) extends Serializable {

  def drainExecutionsTime: EngineResult[T] =
    copy(verdict = this.verdict match {
      case a @ Allowed(erules) => a.copy(evaluatedResults = erules.map(_.drainExecutionTime))
      case a @ Denied(erules)  => a.copy(evaluatedResults = erules.map(_.drainExecutionTime))
    })
}
object EngineResult extends EngineResultInstances {

  def combine[T](data: T, a: EngineResult[T], b: EngineResult[T]): EngineResult[T] =
    EngineResult(
      data = data,
      verdict = (a.verdict, b.verdict) match {
        case (Allowed(rules1), Allowed(rules2)) => Allowed(rules1 ++ rules2.toList)
        case (Denied(rules1), Allowed(_))       => Denied(rules1)
        case (Allowed(_), Denied(rules2))       => Denied(rules2)
        case (Denied(rules1), Denied(rules2))   => Denied(rules1 ++ rules2.toList)
      }
    )

  def combineAll[T](
    data: T,
    er1: EngineResult[T],
    erN: EngineResult[T]*
  ): EngineResult[T] =
    (er1 +: erN).toList.reduce((a, b) => combine(data, a, b))
}

private[erules] trait EngineResultInstances
