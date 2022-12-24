package erules.cats.xml.report

import cats.effect.IO
import cats.Id
import erules.core.{Rule, RulesEngine, RulesEngineIO}
import erules.core.RuleVerdict.Allow
import cats.xml.Xml

class XmlReportEncoderSpec extends munit.CatsEffectSuite {

  import cats.xml.syntax.*
  import cats.xml.generic.encoder.auto.*
  import erules.cats.xml.implicits.*

  test("EngineResult.asXmlReport return a well-formatted Xml report") {
    case class Foo(x: String, y: Int)

    val allowYEqZero: Rule[Id, Foo] = Rule("Check Y value").partially[Id, Foo] { case Foo(_, 0) =>
      Allow.because("because yes!")
    }

    val engine: IO[RulesEngineIO[Foo]] =
      RulesEngine[IO]
        .withRules(allowYEqZero)
        .denyAllNotAllowed

    val result: IO[Xml] =
      engine
        .flatMap(_.parEval(Foo("TEST", 0)))
        .map(_.drainExecutionsTime.asXmlReport)

    assertIO(
      obtained = result,
      returns = xml"""<EngineResult>
                        <Data>
                         <Foo x="TEST" y="0"/>
                        </Data>
                        <InterpreterVerdict type="Allowed">
                         <EvaluatedRules>
                          <RuleResult>
                           <Rule name="Check Y value" description="" targetInfo="">
                            <FullDescription>Check Y value</FullDescription>
                           </Rule>
                           <Verdict type="Allow">
                            <Reasons>
                             <Reason>because yes!</Reason>
                            </Reasons>
                           </Verdict>
                          </RuleResult>
                         </EvaluatedRules>
                        </InterpreterVerdict>
                       </EngineResult>"""
    )
  }

}
