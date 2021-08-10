package erules.core.testings

import cats.{Functor, Id}
import erules.core.{EngineResult, EvalRuleResult, Rule}
import erules.core.EvalRulesInterpreterResult.{Allowed, Denied}

trait ExecutionTimeOps {

  import cats.implicits.*

  implicit class RuleEvaluatedExecutionTimeOps[F[_]: Functor, T, R <: EvalRuleResult](
    fer: F[Rule.TypedEvaluated[T, R]]
  ) {

    def drainExecutionTime: F[Rule.TypedEvaluated[T, R]] =
      fer.map(_.copy(executionTime = None))
  }

  implicit class RuleEvaluatedExecutionTimeOpsForId[T, R <: EvalRuleResult](er: Rule.TypedEvaluated[T, R])
      extends RuleEvaluatedExecutionTimeOps[Id, T, R](er)

  implicit class EngineResultExecutionTimeOps[F[_]: Functor, T](fer: F[EngineResult[T]]) {
    def drainExecutionTime: F[EngineResult[T]] =
      fer.map(er =>
        er.copy(result = er.result match {
          case a @ Allowed(erules) => a.copy(evaluatedRules = erules.drainExecutionTime)
          case a @ Denied(erules)  => a.copy(evaluatedRules = erules.drainExecutionTime)
        })
      )
  }

  implicit class EngineResultExecutionTimeOpsForId[T](er: EngineResult[T])
      extends EngineResultExecutionTimeOps[Id, T](er)
}
