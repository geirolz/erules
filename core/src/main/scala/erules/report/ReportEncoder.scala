package erules.report

import cats.{Functor, Show}

trait ReportEncoder[T, R] {

  def report(t: T): R

  def map[U](f: R => U): ReportEncoder[T, U] =
    ReportEncoder.of(t => f(report(t)))

  def toShow(implicit env: R <:< String): Show[T] =
    (t: T) => report(t)
}
object ReportEncoder extends ReportEncoderInstances with ReportEncoderSyntax {

  def apply[T, R](implicit re: ReportEncoder[T, R]): ReportEncoder[T, R] = re

  def of[T, R](f: T => R): ReportEncoder[T, R] =
    (t: T) => f(t)
}

private[erules] trait ReportEncoderInstances extends StringReportInstances {
  implicit def reportEncoderFunctor[T]: Functor[ReportEncoder[T, *]] =
    new Functor[ReportEncoder[T, *]] {
      override def map[A, B](fa: ReportEncoder[T, A])(f: A => B): ReportEncoder[T, B] = fa.map(f)
    }
}

private[erules] trait ReportEncoderSyntax extends StringReportSyntax {
  implicit class ReportableForAny[T](t: T) {
    def asReport[R](implicit re: ReportEncoder[T, R]): R = re.report(t)
  }
}
