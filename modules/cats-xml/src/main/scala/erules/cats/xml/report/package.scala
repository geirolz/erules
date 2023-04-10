package erules.cats.xml

import cats.xml.Xml
import erules.report.ReportEncoder

package object report {
  type XmlReportEncoder[T] = ReportEncoder[T, Xml]
}
