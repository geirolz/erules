package erules.core.utils

import cats.{Foldable, Order}
import cats.data.NonEmptyList

object CollectionsUtils extends CollectionsUtilsSyntax {

  def findDuplicated[F[_]: Foldable, T, U](xs: F[T], discriminator: T => U): List[T] =
    Foldable[F]
      .foldMap[T, Map[U, List[T]]](xs)(t => Map(discriminator(t) -> List(t)))
      .collect[Option[T]] {
        case (_, _ :: Nil)   => None
        case (_, Nil)        => None
        case (_, duplicates) => Some(duplicates.head)
      }
      .toList
      .flatten

  def findDuplicatedNem[T, U: Order](xs: NonEmptyList[T], discriminator: T => U): List[T] = {
    xs
      .groupBy(discriminator)
      .collect[Option[T]] {
        case (_, NonEmptyList(_, Nil)) => None
        case (_, duplicates)           => Some(duplicates.head)
      }
      .toList
      .flatten
  }
}

trait CollectionsUtilsSyntax {

  implicit class FoldableCustomOps[F[_]: Foldable, T](xs: F[T]) {
    def findDuplicated[U](discriminator: T => U): List[T] =
      CollectionsUtils.findDuplicated[F, T, U](xs, discriminator)
  }

  implicit class NonEmptyOps[T](xs: NonEmptyList[T]) {
    def findDuplicatedNem[U: Order](discriminator: T => U): List[T] =
      CollectionsUtils.findDuplicatedNem[T, U](xs, discriminator)
  }
}
