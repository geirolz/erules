package erules

import erules.core.*
import erules.core.report.{ReportEncoderInstances, ReportEncoderSyntax, StringReportInstances}

object implicits extends AllCoreInstances with AllCoreSyntax

//---------- INSTANCES ----------
object instances extends AllCoreInstances
private[erules] trait AllCoreInstances
    extends EngineResultInstances
    with EvalResultsInterpreterInstances
    with EvalReasonInstances
    with RuleResultInstances
    with RuleVerdictInstances
    with RuleInstances
    with ReportEncoderInstances
    with StringReportInstances

//---------- SYNTAX ----------
object syntax extends AllCoreSyntax
private[erules] trait AllCoreSyntax extends EvalReasonSyntax with ReportEncoderSyntax
