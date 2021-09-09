package erules.core.testings

import cats.effect.kernel.Async
import erules.core.report.ReportEncoder

trait ReportValues {

  import cats.implicits.*

  implicit class ReportEncoderOps[F[_], R <: Serializable, A: ReportEncoder[*, R]](fa: F[A])(implicit F: Async[F]) {
    def logReport: F[A] =
      fa.flatTap(a => F.delay(Console.out.print(ReportEncoder[A, R].report(a))))
  }
}
