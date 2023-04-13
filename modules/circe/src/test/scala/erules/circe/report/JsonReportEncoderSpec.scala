package erules.circe.report

import cats.effect.IO
import erules.{PureRule, Rule, RulesEngine, RulesEngineIO}
import erules.RuleVerdict.Allow
import io.circe.Json

class JsonReportEncoderSpec extends munit.CatsEffectSuite {

  import erules.circe.implicits.*
  import io.circe.generic.auto.*
  import io.circe.literal.*

  test("EngineResult.asJsonReport return a well-formatted JSON report") {
    case class Foo(x: String, y: Int)

    val allowYEqZero: PureRule[Foo] = Rule("Check Y value").partially { case Foo(_, 0) =>
      Allow.because("reason")
    }

    val engine: IO[RulesEngineIO[Foo]] =
      RulesEngine[IO]
        .withRules(allowYEqZero)
        .denyAllNotAllowed[IO]

    val result: IO[Json] =
      engine
        .flatMap(_.parEval(Foo("TEST", 0)))
        .map(_.drainExecutionsTime.asJsonReport)

    assertIO(
      obtained = result,
      returns = json"""
          {
        "data" : {
          "x" : "TEST",
          "y" : 0
        },
        "verdict" : {
          "type" : "Allowed",
          "evaluatedRules" : [
            {
              "ruleInfo" : {
                "name" : "Check Y value",
                "fullDescription" : "Check Y value"
              },
              "verdict" : {
                "type" : "Allow",
                "reasons" : [
                  "reason"
                ]
              }
            }
          ]
        }
      }"""
    )
  }

}
