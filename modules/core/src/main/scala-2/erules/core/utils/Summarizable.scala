package erules.core.utils

trait Summarizable extends Serializable {
  def summary: String
}
object Summarizable {

  val defaultHeaderMaxLen = 60
  val defaultSeparatorSymbol = "-"

  def paragraph(
    title: String,
    epSymbol: String = defaultSeparatorSymbol,
    maxLen: Int = defaultHeaderMaxLen
  )(
    body: String
  ): String =
    s"""|${buildSeparator(title, epSymbol, maxLen)}
        |$body
        |${buildSeparator("", epSymbol, maxLen)}""".stripMargin

  def buildSeparator(
    message: String = "",
    sepSymbol: String = defaultSeparatorSymbol,
    maxLen: Int = defaultHeaderMaxLen
  ): String = {

    val fixedMessage = if (message.isEmpty) "" else s" $message "
    val halfSize: Float = (maxLen - fixedMessage.length) / 2f
    val halfSep: String = (0 until halfSize.toInt).map(_ => sepSymbol).mkString
    val compensator: String = if (halfSize % 2 == 0) "" else sepSymbol

    s"$halfSep$compensator$fixedMessage$halfSep"
  }
}
