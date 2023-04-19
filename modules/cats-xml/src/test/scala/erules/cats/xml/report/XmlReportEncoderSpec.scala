package erules.cats.xml.report

import cats.effect.IO
import cats.xml.{Xml, XmlNode}
import cats.xml.codec.Encoder
import erules.{PureRule, PureRulesEngine, Rule, RulesEngine}
import erules.RuleVerdict.Allow

class XmlReportEncoderSpec extends munit.CatsEffectSuite {

  import cats.xml.implicits.*
  import erules.cats.xml.implicits.*

  test("EngineResult.asXmlReport return a well-formatted Xml report") {

    case class Foo(x: String, y: Int)
    object Foo {
      implicit val xmlEncoder: Encoder[Foo] = Encoder.of(foo =>
        XmlNode("Foo")
          .withAttributes(
            "x" := foo.x,
            "y" := foo.y
          )
      )
    }

    val allowYEqZero: PureRule[Foo] = Rule("Check Y value").partially { case Foo(_, 0) =>
      Allow.because("because yes!")
    }

    val engine: IO[PureRulesEngine[Foo]] =
      RulesEngine
        .withRules(allowYEqZero)
        .denyAllNotAllowed[IO]

    val result: IO[Xml] =
      engine
        .map(_.seqEvalPure(Foo("TEST", 0)))
        .map(_.drainExecutionsTime.asXmlReport)

    assertIO(
      obtained = result,
      returns = xml"""<EngineResult>
                        <Data>
                         <Foo x="TEST" y="0"/>
                        </Data>
                        <Verdict type="Allowed">
                         <EvaluatedRules>
                          <RuleResult>
                           <RuleInfo name="Check Y value" description="" targetInfo="">
                            <FullDescription>Check Y value</FullDescription>
                           </RuleInfo>
                           <Verdict type="Allow">
                            <Reasons>
                             <Reason>because yes!</Reason>
                            </Reasons>
                           </Verdict>
                          </RuleResult>
                         </EvaluatedRules>
                        </Verdict>
                       </EngineResult>"""
    )
  }

}
