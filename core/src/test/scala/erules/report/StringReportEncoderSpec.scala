package erules.report

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import erules.{PureRule, PureRulesEngine, Rule, RulesEngine}
import erules.RuleVerdict.Allow
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class StringReportEncoderSpec extends AsyncWordSpec with AsyncIOSpec with Matchers {

  import erules.implicits.*

  "EngineResult.asReport" should {

    "Return a well-formatted string report" in {

      case class Foo(x: String, y: Int)

      val allowYEqZero: PureRule[Foo] = Rule("Check Y value").partially { case Foo(_, 0) =>
        Allow.withoutReasons
      }

      val engine: IO[PureRulesEngine[Foo]] =
        RulesEngine
          .withRules(allowYEqZero)
          .denyAllNotAllowed[IO]

      val result: IO[String] =
        engine
          .map(_.seqEvalPure(Foo("TEST", 0)))
          .map(_.drainExecutionsTime.asReport[String])

      result
        .asserting(str =>
          str shouldBe
            """|###################### ENGINE VERDICT ######################
               |
               |Data: Foo(TEST,0)
               |Rules: 1
               |Interpreter verdict: Allowed
               |
               |----------------------- Check Y value ----------------------
               |- Rule: Check Y value
               |- Description: 
               |- Target: 
               |- Execution time: *not measured*
               |
               |- Verdict: Right(Allow)
               |
               |------------------------------------------------------------
               |
               |
               |############################################################""".stripMargin
        )
    }
  }

}
