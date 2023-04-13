package erules.utils

import cats.Id
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IsIdSpec extends AnyWordSpec with Matchers {

  import erules.utils.IsId.*

  "IsId.liftId" should {
    "lift A to Id[A]" in {
      1.liftId[Id] shouldBe 1
    }
  }

  "IsId.unliftId" should {
    "unlift Id[A] to A" in {
      Id(1).unliftId shouldBe 1
    }
  }

  "IsId implicit syntax" should {

    "implicitly unliftId F[A] to A when F is Id" in {
      def foo[F[_]: IsId](x: F[Int]): Int = x
      foo[Id](1)
    }

    "unliftId F[A] to A when F is Id" in {
      def foo[F[_]: IsId](x: F[Int]): Int = x.unliftId
      foo[Id](1)
    }

    "liftId A to F[A] when F is Id" in {
      def foo[F[_]: IsId](x: Int): F[Int] = x.liftId[F]
      foo[Id](1)
    }
  }

  "IsId" should {
    "not compile when F is not Id" in {
      """
        |import cats.data.NonEmptyList
        |import erules.utils.IsId
        |
        |implicitly[IsId[NonEmptyList]]
      """.stripMargin shouldNot compile
    }

    "compile when F is Id" in {
      """
        |import erules.utils.IsId
        |
        |implicitly[IsId[Id]]
      """.stripMargin should compile
    }
  }
}
