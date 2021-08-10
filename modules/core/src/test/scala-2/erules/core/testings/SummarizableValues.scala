package erules.core.testings

import cats.effect.kernel.Async
import erules.core.utils.Summarizable

trait SummarizableValues {

  import cats.implicits.*

  implicit class SummarizableConsoleOps[F[_], A <: Summarizable](fa: F[A])(implicit F: Async[F]) {
    def logSummary: F[A] =
      fa.flatTap(a => F.delay(Console.out.print(a.summary)))
  }
}
