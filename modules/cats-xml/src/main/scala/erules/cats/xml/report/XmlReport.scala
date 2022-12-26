package erules.cats.xml.report

import cats.xml.codec.Encoder
import cats.xml.Xml
import erules.core.*

object XmlReport extends XmlReportInstances with XmlReportSyntax {
  def fromEncoder[T: Encoder]: XmlReportEncoder[T] =
    (t: T) => Encoder[T].encode(t)
}
private[xml] trait XmlReportInstances {

  import erules.cats.xml.instances.*

  implicit def engineResultXmlReportEncoder[T: Encoder]: XmlReportEncoder[EngineResult[T]] =
    XmlReport.fromEncoder[EngineResult[T]]

  implicit def ruleResultsInterpreterVerdictXmlReportEncoder[T]
    : XmlReportEncoder[RuleResultsInterpreterVerdict[T]] =
    XmlReport.fromEncoder[RuleResultsInterpreterVerdict[T]]

  implicit def ruleRuleResultXmlReportEncoder[T]
    : XmlReportEncoder[RuleResult[T, ? <: RuleVerdict]] =
    XmlReport.fromEncoder[RuleResult[T, ? <: RuleVerdict]]

  implicit val ruleVerdictXmlReportEncoder: XmlReportEncoder[RuleVerdict] =
    XmlReport.fromEncoder[RuleVerdict]

}

trait XmlReportSyntax {
  implicit class XmlReportEncoderForAny[T](t: T) {
    def asXmlReport(implicit re: XmlReportEncoder[T]): Xml = re.report(t)
  }
}
