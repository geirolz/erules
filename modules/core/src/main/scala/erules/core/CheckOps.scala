package erules.core

import cats.{Foldable, Show}
import cats.implicits._

sealed trait CheckOps {
  val symbol: String
  val show: String
}

sealed trait CheckOps1[A] extends CheckOps {
  def apply(a: A): Boolean
}
sealed trait CheckOps2[A, B] extends CheckOps {
  def apply(a: A, b: B): Boolean
}

//-------------------------- COMMON --------------------------
case class Eq[T: cats.Eq: Show](a: T, b: T) extends CheckOps2[T, T] {
  override val symbol: String = "==="
  override val show: String = s"${a.show} $symbol ${b.show}"

  def apply(a: T, b: T): Boolean = a.eqv(b)
}
case class NotEq[T: cats.Eq: Show](a: T, b: T) extends CheckOps2[T, T] {
  override val symbol: String = "!=="
  override val show: String = s"${a.show} $symbol ${b.show}"

  def apply(a: T, b: T): Boolean = a.neqv(b)
}
case class Gt[T: cats.PartialOrder: Show](a: T, b: T) extends CheckOps2[T, T] {
  override val symbol: String = ">"
  override val show: String = s"${a.show} $symbol ${b.show}"

  def apply(a: T, b: T): Boolean = a > b
}
case class Lt[T: cats.PartialOrder: Show](a: T, b: T) extends CheckOps2[T, T] {
  override val symbol: String = "<"
  override val show: String = s"${a.show} $symbol ${b.show}"

  def apply(a: T, b: T): Boolean = a < b
}
//-------------------------- FOLDABLE --------------------------
case class In[F[_]: Foldable, T: cats.Eq: Show](a: T, f: F[T]) extends CheckOps2[T, F[T]] {
  override val symbol: String = "in"
  override val show: String = s"${a.show} $symbol [${f.mkString_(",")}]"

  def apply(a: T, f: F[T]): Boolean = f.exists(b => cats.Eq[T].eqv(a, b))
}
case class NotIn[F[_]: Foldable, T: cats.Eq: Show](a: T, f: F[T]) extends CheckOps2[T, F[T]] {
  override val symbol: String = "not in"
  override val show: String = s"${a.show} $symbol [${f.mkString_(",")}]"

  def apply(a: T, f: F[T]): Boolean = !f.exists(b => cats.Eq[T].eqv(a, b))
}
