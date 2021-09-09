package erules.core.syntax

import erules.core.report.ReportEncoder

private[syntax] trait ReportEncoderSyntax {
  implicit class ReportableForAny[T](t: T) {
    def asReport[R](implicit re: ReportEncoder[T, R]): R = re.report(t)
  }
}
