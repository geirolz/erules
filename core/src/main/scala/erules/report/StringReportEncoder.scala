package erules.report

import cats.Show
import erules.{EngineResult, EvalReason, RuleResult, RuleResultsInterpreterVerdict}
import erules.RuleResultsInterpreterVerdict.{Allowed, Denied}

object StringReportEncoder extends StringReportInstances with StringReportSyntax {
  val defaultHeaderMaxLen: Int       = 60
  val defaultSeparatorSymbol: String = "-"

  def apply[T](implicit re: StringReportEncoder[T]): StringReportEncoder[T] = re

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

  implicit def stringReportEncoderToShow[T](implicit encoder: StringReportEncoder[T]): Show[T] =
    encoder.toShow

  implicit def stringReportEncoderForEngineResult[T](implicit
    showT: Show[T] = Show.fromToString[T],
    reportEncoderERIR: StringReportEncoder[RuleResultsInterpreterVerdict]
  ): StringReportEncoder[EngineResult[T]] =
    er =>
      StringReportEncoder.paragraph("ENGINE VERDICT", "#")(
        s"""
           |Data: ${showT.show(er.data)}
           |Rules: ${er.verdict.evaluatedResults.size}
           |${reportEncoderERIR.report(er.verdict)}
           |""".stripMargin
      )

  implicit def stringReportEncoderForRuleResultsInterpreterVerdict(implicit
    reportEncoderEvalRule: StringReportEncoder[RuleResult.Unbiased]
  ): StringReportEncoder[RuleResultsInterpreterVerdict] =
    t => {

      val rulesReport: String = t.evaluatedResults
        .map(er =>
          StringReportEncoder.paragraph(er.ruleInfo.fullDescription)(
            reportEncoderEvalRule.report(er)
          )
        )
        .toList
        .mkString("\n")

      val tpe: String = t match {
        case Allowed(_) => "Allowed"
        case Denied(_)  => "Denied"
      }

      s"""Interpreter verdict: $tpe
         |
         |$rulesReport
         |""".stripMargin
    }

  implicit val stringReportEncoderForRuleRuleResult: StringReportEncoder[RuleResult.Unbiased] =
    er => {

      val reasons: String = er.verdict.map(_.reasons) match {
        case Left(ex)       => s"- Failed: $ex"
        case Right(Nil)     => ""
        case Right(reasons) => s"- Because: ${EvalReason.stringifyList(reasons)}"
      }

      s"""|- Rule: ${er.ruleInfo.name}
          |- Description: ${er.ruleInfo.description.getOrElse("")}
          |- Target: ${er.ruleInfo.targetInfo.getOrElse("")}
          |- Execution time: ${er.executionTime
           .map(Show.catsShowForFiniteDuration.show)
           .getOrElse("*not measured*")}
          |
          |- Verdict: ${er.verdict.map(_.typeName)}
          |$reasons""".stripMargin
    }
}

private[erules] trait StringReportSyntax {
  implicit class StringReportEncoderForAny[T](t: T) {
    def asStringReport(implicit re: StringReportEncoder[T]): String = re.report(t)
  }
}
