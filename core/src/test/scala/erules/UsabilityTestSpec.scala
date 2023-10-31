package erules

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.Id
import erules.testings.*
import erules.RuleVerdict.{Allow, Deny}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class UsabilityTestSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers
    with ErulesAsyncAssertingSyntax {

  import erules.report.ReportEncoder.*

  "This library" should {
    "be functional and with a nice syntax" in {

      val returnRules: NonEmptyList[PureRule[Order]] = NonEmptyList.of(
        Rule("ShipTo IT order only") {
          case Order(_, ShipTo(_, Country.`IT`), _, _) => Allow.withoutReasons
          case _                                       => Deny.because("Ship to not to Italy")
        },
        Rule("BillTo UK order only") {
          case Order(_, _, BillTo(_, Country.`UK`), _) => Allow.because(EvalReason("Bill to UK"))
          case _                                       => Deny.because("Bill to not from UK")
        },
        Rule
          .pure[BigDecimal]("Prince under 5k")
          .assert("Be under 5k")(_.toInt < 5000)
          .targetInfo("Total price")
          .contramap(_.items.map(_.price).sum),
        Rule
          .pure[BigDecimal]("Prince under 5k - 2")
          .apply(o => Allow.when(o.toInt < 5000)(Deny.because("Be under 5k")))
          .targetInfo("Total price")
          .contramap(_.items.map(_.price).sum)
      )

      val engine: IO[PureRulesEngine[Order]] =
        RulesEngine
          .withRules(returnRules)
          .denyAllNotAllowed[IO]

      val result: IO[RuleResultsInterpreterVerdict] = engine
        .map(
          _.seqEvalPure(
            Order(
              id     = "123",
              shipTo = ShipTo("Via Roma 1", Country.IT),
              billTo = BillTo("Via Roma 1", Country.IT),
              items  = List(Item("123", 1, BigDecimal(10)))
            )
          )
        )
        .flatTap(_.printStringReport[IO])
        .map(_.verdict)

      result.asserting(_.isAllowed shouldBe false)
    }
  }
}
