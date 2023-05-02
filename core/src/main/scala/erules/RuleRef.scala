package erules

import cats.{Eq, Show}
import cats.kernel.Hash

import java.math.BigInteger

/** A unique reference to a rule.
  */
final class RuleRef private[erules] (val value: String) extends AnyVal with Serializable
object RuleRef {

  def fromString(value: String): RuleRef =
    new RuleRef(new BigInteger(value.getBytes()).toString())

  implicit val eq: Eq[RuleRef]     = Eq.by(_.value)
  implicit val show: Show[RuleRef] = Show.fromToString[RuleRef]
  implicit val hash: Hash[RuleRef] = Hash.fromUniversalHashCode[RuleRef]
}
