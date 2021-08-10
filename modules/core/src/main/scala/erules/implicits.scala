package erules

import erules.core.{EvalResultReasonInstances, EvalResultsInterpreterInstances, RuleInstances}
import erules.core.syntax.AllCoreSyntax

object implicits extends AllCoreInstances with AllCoreSyntax

private[erules] trait AllCoreInstances
    extends EvalResultReasonInstances
    with EvalResultsInterpreterInstances
    with RuleInstances
