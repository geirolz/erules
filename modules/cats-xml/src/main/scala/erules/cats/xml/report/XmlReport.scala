package erules.cats.xml.report

import cats.xml.codec.Encoder
import cats.xml.Xml
import erules.{EngineResult, RuleResult, RuleResultsInterpreterVerdict, RuleVerdict}

object XmlReport extends XmlReportInstances with XmlReportSyntax {
  def fromEncoder[T: Encoder]: XmlReportEncoder[T] =
    (t: T) => Encoder[T].encode(t)
}
private[xml] trait XmlReportInstances {

  import erules.cats.xml.instances.*

  implicit def engineResultXmlReportEncoder[T: Encoder]: XmlReportEncoder[EngineResult[T]] =
    XmlReport.fromEncoder[EngineResult[T]]

  implicit val ruleResultsInterpreterVerdictXmlReportEncoder
    : XmlReportEncoder[RuleResultsInterpreterVerdict] =
    XmlReport.fromEncoder[RuleResultsInterpreterVerdict]

  implicit val ruleRuleResultXmlReportEncoder: XmlReportEncoder[RuleResult[? <: RuleVerdict]] =
    XmlReport.fromEncoder[RuleResult[? <: RuleVerdict]]

  implicit val ruleVerdictXmlReportEncoder: XmlReportEncoder[RuleVerdict] =
    XmlReport.fromEncoder[RuleVerdict]

}

trait XmlReportSyntax {
  implicit class XmlReportEncoderForAny[T](t: T) {
    def asXmlReport(implicit re: XmlReportEncoder[T]): Xml = re.report(t)
  }
}
