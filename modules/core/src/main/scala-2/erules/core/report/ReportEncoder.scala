package erules.core.report

import cats.Show

trait ReportEncoder[T, R] {

  def report(t: T): R

  def toShow(implicit env: R <:< String): Show[T] =
    (t: T) => report(t)
}
object ReportEncoder {

  def apply[T, R](implicit re: ReportEncoder[T, R]): ReportEncoder[T, R] = re

  def of[T, R](f: T => R): ReportEncoder[T, R] =
    (t: T) => f(t)
}
