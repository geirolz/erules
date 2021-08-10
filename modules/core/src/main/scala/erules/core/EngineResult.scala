package erules.core

/** Describes the engine output.
  * @param data Data used for the evaluation
  * @param result Final engine result, the decision taken by then interpreter using the list of evaluated rules.
  * @tparam T Data type
  */
case class EngineResult[T](
  data: T,
  result: EngineEvalResult[T]
)

/** ADT to define the possible output of the engine evaluation.
  */
sealed trait EngineEvalResult[-T] {

  /** Result reasons
    */
  val evaluatedRules: List[Rule.Evaluated[T]]
}
object EngineEvalResult {
  case class EngineAllow[T](evaluatedRules: List[Rule.TypedEvaluated[T, RuleEvalResult.Allow]])
      extends EngineEvalResult[T]
  case class EngineDeny[T](evaluatedRules: List[Rule.TypedEvaluated[T, RuleEvalResult.Deny]])
      extends EngineEvalResult[T]
}
