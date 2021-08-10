package erules.core.testings

import cats.effect.IO
import cats.Show
import erules.core.utils.Summarizable

private[core] trait DebuggingIOConsole {

  implicit class DebuggingIOConsoleOps[A](ioa: IO[A]) {

    def logShow(implicit sa: Show[A]): IO[A] =
      ioa.flatTap(a => IO(Console.out.print(Show[A].show(a))))

    def logSummary(implicit eq: A <:< Summarizable): IO[A] =
      ioa.flatTap(a => IO(Console.out.print(a.summary)))

    def log: IO[A] =
      ioa.flatTap(a => IO(Console.out.print(a.toString)))
  }
}
