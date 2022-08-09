package erules.circe

import erules.core.*

object instances extends ErulesTypesCirceInstances
private[erules] trait ErulesTypesCirceInstances {

  import erules.circe.GenericCirceInstances.*
  import io.circe.*
  import io.circe.syntax.*

  implicit def engineResultCirceEncoder[T: Encoder]: Encoder[EngineResult[T]] =
    io.circe.generic.semiauto.deriveEncoder[EngineResult[T]]

  implicit def ruleResultsInterpreterCirceEncoder[T]: Encoder[RuleResultsInterpreterVerdict[T]] =
    Encoder.instance { v =>
      Json.obj(
        "type"           -> Json.fromString(v.typeName),
        "evaluatedRules" -> Json.fromValues(v.evaluatedRules.toList.map(_.asJson))
      )
    }

  implicit def ruleResultCirceEncoder[T]: Encoder[RuleResult[T, RuleVerdict]] =
    io.circe.generic.semiauto.deriveEncoder[RuleResult[T, RuleVerdict]]

  implicit def ruleCirceEncoder[T]: Encoder[AnyTypedRule[T]] =
    Encoder.instance { v =>
      Json.obj(
        "name"            -> Json.fromString(v.name),
        "description"     -> v.description.map(Json.fromString).getOrElse(Json.Null),
        "targetInfo"      -> v.targetInfo.map(Json.fromString).getOrElse(Json.Null),
        "fullDescription" -> Json.fromString(v.fullDescription)
      )
    }

  implicit final val ruleVerdictCirceEncoder: Encoder[RuleVerdict] =
    Encoder.instance { v =>
      Json.obj(
        "type"    -> Json.fromString(v.typeName),
        "reasons" -> Json.fromValues(v.reasons.map(_.asJson))
      )
    }

  implicit final val evalReasonCirceEncoder: Encoder[EvalReason] =
    io.circe.generic.semiauto.deriveEncoder[EvalReason]
}
