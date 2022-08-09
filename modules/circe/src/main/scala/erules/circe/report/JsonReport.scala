package erules.circe.report

import erules.core.*
import io.circe.{Encoder, Json}

object JsonReport extends JsonReportInstances with JsonReportSyntax {
  def fromEncoder[T: Encoder]: JsonReportEncoder[T] =
    (t: T) => Encoder[T].apply(t).deepDropNullValues
}
private[circe] trait JsonReportInstances {

  import erules.circe.instances.*

  implicit def engineResultJsonReportEncoder[T: Encoder]: JsonReportEncoder[EngineResult[T]] =
    JsonReport.fromEncoder[EngineResult[T]]

  implicit def ruleResultsInterpreterVerdictJsonReportEncoder[T]
    : JsonReportEncoder[RuleResultsInterpreterVerdict[T]] =
    JsonReport.fromEncoder[RuleResultsInterpreterVerdict[T]]

  implicit def ruleRuleResultJsonReportEncoder[T]
    : JsonReportEncoder[RuleResult[T, ? <: RuleVerdict]] =
    JsonReport.fromEncoder[RuleResult[T, ? <: RuleVerdict]]

  implicit val ruleVerdictJsonReportEncoder: JsonReportEncoder[RuleVerdict] =
    JsonReport.fromEncoder[RuleVerdict]

}

private[circe] trait JsonReportSyntax {
  implicit class JsonReportEncoderForAny[T](t: T) {
    def asJsonReport(implicit re: JsonReportEncoder[T]): Json = re.report(t)
  }
}
