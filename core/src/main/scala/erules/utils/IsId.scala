package erules.utils

import cats.{Applicative, Id}

@annotation.implicitNotFound("${F} is not cats.Id")
trait IsId[F[_]] {
  def liftId[A](a: A): F[A]
  def unliftId[A](fa: F[A]): A
  def applicative: Applicative[F]
}
object IsId {

  def apply[F[_]: IsId]: IsId[F] = implicitly[IsId[F]]

  implicit def isIdInstance[F[X] <: Id[X]: Applicative]: IsId[F] = new IsId[F] {
    override def unliftId[A](fa: F[A]): A    = fa
    override def liftId[A](a: A): F[A]       = applicative.pure(a)
    override def applicative: Applicative[F] = Applicative[F]
  }

  implicit def unliftIdConversion[F[_]: IsId, A](fa: F[A]): A = IsId[F].unliftId(fa)
  implicit def liftIdConversion[A](fa: A): Id[A]              = IsId[Id].unliftId(fa)

  implicit class IsIdUnliftOps[F[_]: IsId, A](fa: F[A]) {
    def unliftId: A = IsId[F].unliftId(fa)
  }

  implicit class IsIdLiftOps[A](a: A) {
    def liftId[F[_]: IsId]: F[A] = IsId[F].liftId(a)
  }
}
