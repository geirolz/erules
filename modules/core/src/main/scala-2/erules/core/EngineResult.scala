package erules.core

import cats.Show
import erules.core.EvalRulesInterpreterResult.{Allowed, Denied}
import erules.core.utils.Summarizable

/** Describes the engine output.
  *
  * @param data
  *   Data used for the evaluation
  * @param result
  *   Final engine result, the decision taken by then interpreter using the report.
  * @tparam T
  *   Data type
  */
case class EngineResult[T](
  data: T,
  result: EvalRulesInterpreterResult[T]
) extends Summarizable {

  override def summary: String = {
    implicit val showT: Show[T] = cats.Show.fromToString[T]
    implicitly[Show[EngineResult[T]]].show(this)
  }
}
object EngineResult {

  implicit def catsShowInstanceForEngineResult[T](implicit
    showT: Show[T],
    showERIR: Show[EvalRulesInterpreterResult[T]]
  ): Show[EngineResult[T]] =
    er =>
      Summarizable.paragraph("ENGINE RESULT", "#")(
        s"""
           |Data: ${showT.show(er.data)}
           |Rules: ${er.result.evaluatedRules.size}
           |${showERIR.show(er.result)}
           |""".stripMargin
      )

  def combine[T](data: T, a: EngineResult[T], b: EngineResult[T]): EngineResult[T] =
    EngineResult(
      data = data,
      result = (a.result, b.result) match {
        case (Allowed(rules1), Allowed(rules2)) => Allowed(rules1 ++ rules2.toList)
        case (Denied(rules1), Allowed(_))       => Denied(rules1)
        case (Allowed(_), Denied(rules2))       => Denied(rules2)
        case (Denied(rules1), Denied(rules2))   => Denied(rules1 ++ rules2.toList)
      }
    )

  def combineAll[T](data: T, er1: EngineResult[T], erN: EngineResult[T]*): EngineResult[T] =
    (er1 +: erN).toList.reduce((a, b) => combine(data, a, b))
}
