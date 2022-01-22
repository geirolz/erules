package erules.core

package object report {
  type StringReport[T] = ReportEncoder[T, String]
}
