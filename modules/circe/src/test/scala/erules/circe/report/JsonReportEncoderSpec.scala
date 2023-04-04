package erules.circe.report

import cats.effect.IO
import cats.Id
import erules.core.{Rule, RulesEngine, RulesEngineIO}
import erules.core.RuleVerdict.Allow
import io.circe.Json

class JsonReportEncoderSpec extends munit.CatsEffectSuite {

  import erules.circe.implicits.*
  import io.circe.generic.auto.*
  import io.circe.literal.*

  test("EngineResult.asJsonReport return a well-formatted JSON report") {
    case class Foo(x: String, y: Int)

    val allowYEqZero: Rule[Id, Foo] = Rule("Check Y value").partially[Id, Foo] { case Foo(_, 0) =>
      Allow.because("reason")
    }

    val engine: IO[RulesEngineIO[Foo]] =
      RulesEngine[IO]
        .withRules(allowYEqZero)
        .denyAllNotAllowed

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
                "ref" : "5340595900475325933418219074917",
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
