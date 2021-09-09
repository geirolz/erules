package erules

import erules.core.*
import erules.core.report.StringReportInstances
import erules.core.syntax.AllCoreSyntax

object implicits extends AllCoreInstances with AllCoreSyntax

private[erules] trait AllCoreInstances
    extends EngineResultInstances
    with EvalResultsInterpreterInstances
    with EvalReasonInstances
    with RuleResultInstances
    with RuleVerdictInstances
    with RuleInstances
    with StringReportInstances
