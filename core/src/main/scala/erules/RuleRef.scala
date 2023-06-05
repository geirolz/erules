package erules

import cats.{Eq, Show}
import cats.kernel.Hash

import java.nio.charset.StandardCharsets

/** A unique reference to a rule.
  */
final class RuleRef private[erules] (val value: BigInt) extends AnyVal with Serializable {
  override def toString: String = value.toString()
}
object RuleRef {

  def fromString(value: String): RuleRef = {
    val len         = 8
    val base        = if (value.isEmpty) "0" else value
    val bytes       = base.getBytes(StandardCharsets.UTF_8).slice(0, len)
    val fillerBytes = Array.fill[Byte](len - bytes.length)(0)
    val filledBytes = bytes ++ fillerBytes
    new RuleRef(BigInt(filledBytes).abs)
  }

  implicit val eq: Eq[RuleRef]     = Eq.by(_.value)
  implicit val show: Show[RuleRef] = Show.fromToString[RuleRef]
  implicit val hash: Hash[RuleRef] = Hash.fromUniversalHashCode[RuleRef]
}
