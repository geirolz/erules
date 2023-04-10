package erules.circe

import erules.*
import erules.circe.report.{JsonReportInstances, JsonReportSyntax}

object implicits extends CirceAllInstances with CirceAllSyntax

//---------- INSTANCES ----------
object instances extends CirceAllInstances
private[circe] trait CirceAllInstances extends BasicTypesCirceInstances with JsonReportInstances

private[circe] trait BasicTypesCirceInstances {

  import erules.circe.GenericCirceInstances.*
  import io.circe.*
  import io.circe.syntax.*

  implicit final val evalReasonCirceEncoder: Encoder[EvalReason] =
    Encoder.encodeString.contramap(_.message)

  implicit def engineResultCirceEncoder[T: Encoder]: Encoder[EngineResult[T]] =
    io.circe.generic.semiauto.deriveEncoder[EngineResult[T]]

  implicit final val ruleResultsInterpreterCirceEncoder: Encoder[RuleResultsInterpreterVerdict] =
    Encoder.instance { v =>
      Json.obj(
        "type"           -> Json.fromString(v.typeName),
        "evaluatedRules" -> Json.fromValues(v.evaluatedResults.toList.map(_.asJson))
      )
    }

  implicit final val ruleInfoCirceEncoder: Encoder[RuleInfo] =
    Encoder.instance { v =>
      Json.obj(
        "name"            -> Json.fromString(v.name),
        "description"     -> v.description.map(Json.fromString).getOrElse(Json.Null),
        "targetInfo"      -> v.targetInfo.map(Json.fromString).getOrElse(Json.Null),
        "fullDescription" -> Json.fromString(v.fullDescription)
      )
    }

  implicit final def ruleResultCirceEncoder[V <: RuleVerdict: Encoder]: Encoder[RuleResult[V]] = {
    Encoder.instance { v =>
      Json.obj(
        "ruleInfo"      -> v.ruleInfo.asJson,
        "verdict"       -> v.verdict.asJson,
        "executionTime" -> v.executionTime.asJson
      )
    }
  }

  implicit final val ruleVerdictCirceEncoder: Encoder[RuleVerdict] =
    Encoder.instance { v =>
      Json.obj(
        "type"    -> Json.fromString(v.typeName),
        "reasons" -> Json.fromValues(v.reasons.map(_.asJson))
      )
    }
}

//---------- SYNTAX ----------
object syntax extends CirceAllSyntax
private[circe] trait CirceAllSyntax extends JsonReportSyntax
