package erules

import cats.{Eq, Show}
import cats.kernel.Hash

/** A unique reference to a rule.
  */
final class RuleRef private[erules] (val value: BigInt) extends AnyVal with Serializable
object RuleRef {

  def fromString(value: String): RuleRef = {

    val withoutSpaces: String = value.filter(_ != ' ')
    val withoutSpacesLen      = withoutSpaces.length
    val res =
      s"$withoutSpacesLen${value.length - withoutSpacesLen}${BigInt(withoutSpaces.getBytes)}"
    new RuleRef(BigInt(res.getBytes.slice(0, 64)))
  }

  implicit val eq: Eq[RuleRef]     = Eq.by(_.value)
  implicit val show: Show[RuleRef] = Show.fromToString[RuleRef]
  implicit val hash: Hash[RuleRef] = Hash.fromUniversalHashCode[RuleRef]
}
