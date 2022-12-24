package erules.cats.xml

import cats.xml.Xml
import erules.core.report.ReportEncoder

package object report {
  type XmlReportEncoder[T] = ReportEncoder[T, Xml]
}
