package erules.core.syntax

import erules.core.EvalResultReason

private[erules] trait AllCoreSyntax

private[syntax] sealed trait EvalResultReasonSyntax {
  implicit class EvalResultReasonStringOps(private val ctx: StringContext) {
    def er(args: Any*): EvalResultReason = EvalResultReason(ctx.s(args))
  }
}
