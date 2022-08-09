package erules.core

import cats.Show

/** Value class for a simple reason message.
  *
  * @param message
  *   reason message
  */
case class EvalReason(message: String)
object EvalReason extends EvalReasonInstances with EvalReasonSyntax {

  def stringifyList(reasons: List[EvalReason]): String =
    reasons match {
      case Nil           => ""
      case reason :: Nil => Show[EvalReason].show(reason)
      case allReasons    => allReasons.map(r => s"-${Show[EvalReason].show(r)}").mkString("\n")
    }
}

private[erules] trait EvalReasonInstances {
  implicit val showInstanceForEvalReason: Show[EvalReason] = _.message
}

private[erules] trait EvalReasonSyntax {
  implicit class EvalResultReasonStringOps(private val ctx: StringContext) {
    def er(args: Any*): EvalReason = EvalReason(ctx.s(args))
  }
}
