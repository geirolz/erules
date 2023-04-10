package erules.circe

import erules.report.ReportEncoder
import io.circe.Json

package object report {
  type JsonReportEncoder[T] = ReportEncoder[T, Json]
}
