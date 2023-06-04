package erules

import cats.{Eq, Show}
import cats.kernel.Hash

/** A unique reference to a rule.
  */
final class RuleRef private[erules] (val value: BigInt) extends AnyVal with Serializable
object RuleRef {

  def fromString(value: String): RuleRef =
    new RuleRef(BigInt(value.getBytes()))

  implicit val eq: Eq[RuleRef]     = Eq.by(_.value)
  implicit val show: Show[RuleRef] = Show.fromToString[RuleRef]
  implicit val hash: Hash[RuleRef] = Hash.fromUniversalHashCode[RuleRef]
}
