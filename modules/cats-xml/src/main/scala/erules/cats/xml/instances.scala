package erules.cats.xml

import cats.xml.{XmlData, XmlNode}
import cats.xml.codec.Encoder
import erules.cats.xml.report.{XmlReportInstances, XmlReportSyntax}
import erules.core.*

import scala.concurrent.duration.FiniteDuration

//import scala.concurrent.duration.FiniteDuration

object implicits extends CatsXmlAllInstances with CatsXmlAllSyntax

//---------- INSTANCES ----------
object instances extends CatsXmlAllInstances
private[xml] trait CatsXmlAllInstances extends BasicTypesCatsXmlInstances with XmlReportInstances

private[xml] trait BasicTypesCatsXmlInstances {

  import cats.xml.syntax.*
  import erules.cats.xml.GenericCatsXmlInstances.*

  implicit final val evalReasonCatsXmlEncoder: Encoder[EvalReason] =
    Encoder.of(reason => XmlNode("Reason").withText(reason.message))

  implicit def engineResultCatsXmlEncoder[T: Encoder]: Encoder[EngineResult[T]] =
    Encoder.of[EngineResult[T]](res => {
      XmlNode("EngineResult")
        .withChildren(
          List(
            XmlNode("Data").withChildren(Encoder[T].encode(res.data).asNode.toList)
          ) ++ Encoder[RuleResultsInterpreterVerdict[T]].encode(res.verdict).asNode.toList
        )
    })

  implicit def ruleResultsInterpreterCatsXmlEncoder[T]
    : Encoder[RuleResultsInterpreterVerdict[T]] = {
    Encoder.of { v =>
      XmlNode("Verdict")
        .withAttributes(
          "type" := v.typeName
        )
        .withChildren(
          XmlNode("EvaluatedRules").withChildren(v.evaluatedRules.toList.flatMap(_.toXml.asNode))
        )
    }
  }

  implicit def ruleResultCatsXmlEncoder[T]: Encoder[RuleResult[T, RuleVerdict]] = {
    Encoder.of[RuleResult[T, RuleVerdict]](result => {
      XmlNode("RuleResult")
        .withChildren(
          List(
            Encoder[AnyTypedRule[T]].encode(result.rule).asNode,
            Encoder[EitherThrow[RuleVerdict]].encode(result.verdict).asNode,
            result.executionTime
              .flatMap(Encoder[FiniteDuration].encode(_).asNode)
              .map(duration => XmlNode("ExecutionTime").withChildren(duration))
          ).flatten
        )
    })
  }

  implicit def ruleCatsXmlEncoder[T]: Encoder[AnyTypedRule[T]] =
    Encoder.of { v =>
      XmlNode("Rule")
        .withAttributes(
          "name" := v.name,
          "description" := v.description.map(XmlData.fromString).getOrElse(XmlData.empty),
          "targetInfo" := v.targetInfo.map(XmlData.fromString).getOrElse(XmlData.empty)
        )
        .withChildren(
          XmlNode("FullDescription").withText(v.fullDescription)
        )
    }

  implicit final val ruleVerdictCatsXmlEncoder: Encoder[RuleVerdict] =
    Encoder.of { v =>
      XmlNode("Verdict")
        .withAttributes("type" := v.typeName)
        .withChildren(
          XmlNode("Reasons")
            .withChildren(
              v.reasons.flatMap(_.toXml.asNode)
            )
        )
    }
}

//---------- SYNTAX ----------
object syntax extends CatsXmlAllSyntax
private[xml] trait CatsXmlAllSyntax extends XmlReportSyntax
