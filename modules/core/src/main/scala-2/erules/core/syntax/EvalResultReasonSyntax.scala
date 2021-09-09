package erules.core.syntax

import erules.core.EvalReason
import erules.core.Rule.RuleBuilder

private[syntax] trait EvalResultReasonSyntax {

  implicit class RuleBuilderStringOps(private val ctx: StringContext) {
    def r(args: Any*): RuleBuilder = new RuleBuilder(ctx.s(args))
  }

  implicit class EvalResultReasonStringOps(private val ctx: StringContext) {
    def er(args: Any*): EvalReason = EvalReason(ctx.s(args))
  }
}
