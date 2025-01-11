package erules.circe.report

import cats.effect.IO
import erules.{PureRule, Rule, RulesEngine, RulesEngineIO}
import erules.RuleVerdict.Allow
import io.circe.Json
import io.circe.parser.parse

class JsonReportEncoderSpec extends munit.CatsEffectSuite {

  import erules.circe.implicits.*
  import io.circe.generic.auto.*

  test("EngineResult.asJsonReport return a well-formatted JSON report") {
    case class Foo(x: String, y: Int)

    val allowYEqZero: PureRule[Foo] = Rule("Check Y value").matchOrIgnore { case Foo(_, 0) =>
      Allow.because("reason")
    }

    val engine: IO[RulesEngineIO[Foo]] =
      RulesEngine
        .withRules(allowYEqZero)
        .liftK[IO]
        .denyAllNotAllowed[IO]

    val result: IO[Json] =
      engine
        .flatMap(_.parEval(Foo("TEST", 0)))
        .map(_.drainExecutionsTime.asJsonReport)

    val jsonString =
      """
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

    val expectation =
      parse(jsonString).fold(failure => throw new Exception(failure.message), identity)

    assertIO(
      obtained = result,
      returns  = expectation
    )
  }

}
