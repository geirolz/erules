package erules.core.report

import cats.Show

object StringReport extends StringReportInstances {
  val defaultHeaderMaxLen: Int       = 60
  val defaultSeparatorSymbol: String = "-"

  def apply[T](implicit re: StringReport[T]): StringReport[T] = re

  def fromShow[T: Show]: StringReport[T] =
    (t: T) => Show[T].show(t)

  def paragraph(
    title: String,
    epSymbol: String = defaultSeparatorSymbol,
    maxLen: Int      = defaultHeaderMaxLen
  )(
    body: String
  ): String =
    s"""|${buildSeparatorAsString(title, epSymbol, maxLen)}
        |$body
        |${buildSeparatorAsString("", epSymbol, maxLen)}""".stripMargin

  def buildSeparatorAsString(
    message: String   = "",
    sepSymbol: String = defaultSeparatorSymbol,
    maxLen: Int       = defaultHeaderMaxLen
  ): String = {

    val fixedMessage        = if (message.isEmpty) "" else s" $message "
    val halfSize: Float     = (maxLen - fixedMessage.length) / 2f
    val halfSep: String     = (0 until halfSize.toInt).map(_ => sepSymbol).mkString
    val compensator: String = if (halfSize % 2 == 0) "" else sepSymbol

    s"$halfSep$compensator$fixedMessage$halfSep"
  }
}
private[erules] trait StringReportInstances {
  implicit def deriveStringReportFromShow[T: Show]: StringReport[T] = StringReport.fromShow[T]
}
