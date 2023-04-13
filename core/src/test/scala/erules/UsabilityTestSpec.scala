package erules

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import erules.testings.*
import erules.RuleVerdict.{Allow, Deny}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class UsabilityTestSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers
    with ErulesAsyncAssertingSyntax {

  "This library" should {
    "be functional and with a nice syntax" in {

      val returnRules: NonEmptyList[PureRule[Order]] = NonEmptyList.of(
        Rule("ShipTo IT order only") {
          case Order(_, ShipTo(_, Country.`IT`), _, _) => Allow.withoutReasons
          case _                                       => Deny.because("Ship to not to Italy")
        },
        Rule("BillTo UK order only") {
          case Order(_, _, BillTo(_, Country.`UK`), _) => Allow.withoutReasons
          case _                                       => Deny.because("Bill to not from UK")
        },
        Rule
          .pure[BigDecimal]("Prince under 5k") {
            case x if x.toInt < 5000 => Allow.withoutReasons
            case _                   => Deny.because("Bill to not from UK")
          }
          .contramap(_.items.map(_.price).sum)
      )

      val engine: IO[PureRulesEngine[Order]] =
        RulesEngine.pure
          .withRules(returnRules)
          .denyAllNotAllowed[IO]

      engine
        .map(
          _.pureSeqEval(
            Order(
              id     = "123",
              shipTo = ShipTo("Via Roma 1", Country.IT),
              billTo = BillTo("Via Roma 1", Country.IT),
              items  = List(Item("123", 1, BigDecimal(10)))
            )
          )
        )
        .asserting(_.verdict.isAllowed shouldBe false)
    }
  }
}
