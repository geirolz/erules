package erules.circe.report

import erules.{EngineResult, RuleResult, RuleResultsInterpreterVerdict, RuleVerdict}
import io.circe.{Encoder, Json}

object JsonReport extends JsonReportInstances with JsonReportSyntax {
  def fromEncoder[T: Encoder]: JsonReportEncoder[T] =
    (t: T) => Encoder[T].apply(t).deepDropNullValues
}
private[circe] trait JsonReportInstances {

  import erules.circe.instances.*

  implicit def engineResultJsonReportEncoder[T: Encoder]: JsonReportEncoder[EngineResult[T]] =
    JsonReport.fromEncoder[EngineResult[T]]

  implicit final val ruleResultsInterpreterVerdictJsonReportEncoder
    : JsonReportEncoder[RuleResultsInterpreterVerdict] =
    JsonReport.fromEncoder[RuleResultsInterpreterVerdict]

  implicit final val ruleRuleResultJsonReportEncoder
    : JsonReportEncoder[RuleResult[? <: RuleVerdict]] =
    JsonReport.fromEncoder[RuleResult[? <: RuleVerdict]]

  implicit final val ruleVerdictJsonReportEncoder: JsonReportEncoder[RuleVerdict] =
    JsonReport.fromEncoder[RuleVerdict]
}

private[circe] trait JsonReportSyntax {
  implicit class JsonReportEncoderForAny[T](t: T) {
    def asJsonReport(implicit re: JsonReportEncoder[T]): Json = re.report(t)
  }
}
