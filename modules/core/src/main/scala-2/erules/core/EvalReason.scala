package erules.core

import cats.Show
import erules.core.utils.Summarizable

/** Value class for a simple reason message.
  *
  * @param message
  *   reason message
  */
case class EvalReason(message: String) extends Summarizable {
  override def summary: String = Show[EvalReason].show(this)
}
object EvalReason extends EvalReasonInstances {

  def stringifyList(reasons: List[EvalReason]): String =
    reasons match {
      case Nil           => ""
      case reason :: Nil => Show[EvalReason].show(reason)
      case allReasons    => allReasons.map(r => s"-${Show[EvalReason].show(r)}").mkString("\n")
    }
}

private[erules] trait EvalReasonInstances {
  implicit val showInstanceForEvalReason: Show[EvalReason] =
    r => s"${r.message}"
}
