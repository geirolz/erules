package erules

import erules.core.{EvalResultsInterpreterInstances, RuleInstances}
import erules.core.syntax.AllCoreSyntax

object implicits extends AllCoreInstances with AllCoreSyntax

private[erules] trait AllCoreInstances extends EvalResultsInterpreterInstances with RuleInstances
