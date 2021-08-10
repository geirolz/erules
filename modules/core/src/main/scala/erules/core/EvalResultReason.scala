package erules.core

import cats.Show

/** Value class for a simple reason message.
  *
  * @param message reason message
  */
case class EvalResultReason(message: String) extends AnyVal {
  final override def toString: String = Show[EvalResultReason].show(this)
}
object EvalResultReason extends EvalResultReasonInstances

private[erules] trait EvalResultReasonInstances {
  implicit val showInstanceForEvalResultReason: Show[EvalResultReason] =
    r => s"'${r.message}'"
}
