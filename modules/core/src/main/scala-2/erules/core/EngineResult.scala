package erules.core

import cats.Show
import erules.core.RuleResultsInterpreterVerdict.{Allowed, Denied}
import erules.core.utils.Summarizable

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
  verdict: RuleResultsInterpreterVerdict[T]
) extends Summarizable {

  def drainExecutionsTime: EngineResult[T] =
    copy(verdict = this.verdict match {
      case a @ Allowed(erules) => a.copy(evaluatedRules = erules.map(_.drainExecutionTime))
      case a @ Denied(erules)  => a.copy(evaluatedRules = erules.map(_.drainExecutionTime))
    })

  override def summary: String = {
    implicit val showT: Show[T] = cats.Show.fromToString[T]
    implicitly[Show[EngineResult[T]]].show(this)
  }
}
object EngineResult {

  implicit def catsShowInstanceForEngineResult[T](implicit
    showT: Show[T],
    showERIR: Show[RuleResultsInterpreterVerdict[T]]
  ): Show[EngineResult[T]] =
    er =>
      Summarizable.paragraph("ENGINE VERDICT", "#")(
        s"""
           |Data: ${showT.show(er.data)}
           |Rules: ${er.verdict.evaluatedRules.size}
           |${showERIR.show(er.verdict)}
           |""".stripMargin
      )

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

  def combineAll[T](data: T, er1: EngineResult[T], erN: EngineResult[T]*): EngineResult[T] =
    (er1 +: erN).toList.reduce((a, b) => combine(data, a, b))
}
