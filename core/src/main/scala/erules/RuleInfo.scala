package erules

import cats.kernel.Eq

case class RuleInfo(
  name: String,
  description: Option[String] = None,
  targetInfo: Option[String]  = None
) {

  /** Unique rule reference
    */
  // noinspection ScalaWeakerAccess
  lazy val uniqueRef: RuleRef = RuleRef.fromString(fullDescription)

  /** A full description of the rule, that contains name, description and target info where defined.
    */
  def fullDescription: String = {
    (description, targetInfo) match {
      case (None, None)                          => name
      case (Some(description), None)             => s"$name: $description"
      case (None, Some(targetInfo))              => s"$name for $targetInfo"
      case (Some(description), Some(targetInfo)) => s"$name for $targetInfo: $description"
    }
  }
}
object RuleInfo {
  implicit val eq: Eq[RuleInfo] = Eq.by(_.uniqueRef)
}
