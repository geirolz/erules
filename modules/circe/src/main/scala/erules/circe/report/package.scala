package erules.circe

import erules.core.report.ReportEncoder
import io.circe.Json

package object report {
  type JsonReportEncoder[T] = ReportEncoder[T, Json]
}
