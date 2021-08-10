package erules.core

import erules.core.RuleEvalResult.{Allow, Deny, Ignore}

trait EvalResultsInterpreter[T] {
  def interpret(report: List[Rule.Evaluated[T]]): EngineEvalResult[T]
}
object EvalResultsInterpreter extends EvalResultsInterpreterInstances {
  object Defaults {
    def allowAllNotDenied[T]: EvalResultsInterpreter[T] = new AllowAllNotDeniedEvalResultsInterpreter[T]()
    def denyAllNotAllowed[T]: EvalResultsInterpreter[T] = new DenyAllNotAllowedEvalResultsInterpreter[T]()
  }
}

private[erules] trait EvalResultsInterpreterInstances {

  class AllowAllNotDeniedEvalResultsInterpreter[T] extends EvalResultsInterpreter[T] {
    override def interpret(report: List[Rule.Evaluated[T]]): EngineEvalResult[T] =
      partialEval(report).getOrElse(
        EngineEvalResult.EngineAllow(
          List(
            Rule.TypedEvaluated.noMatch(Allow.because("Allow All Not Explicitly Denied"))
          )
        )
      )
  }

  class DenyAllNotAllowedEvalResultsInterpreter[T] extends EvalResultsInterpreter[T] {
    override def interpret(report: List[Rule.Evaluated[T]]): EngineEvalResult[T] =
      partialEval(report).getOrElse(
        EngineEvalResult.EngineDeny(
          List(
            Rule.TypedEvaluated.noMatch(Deny.because("Deny All Not Explicitly Allowed"))
          )
        )
      )
  }

  private def partialEval[T](report: List[Rule.Evaluated[T]]): Option[EngineEvalResult[T]] = {
    type Eval[+R <: RuleEvalResult] = Rule.TypedEvaluated[T, R]

    report
      .flatMap {
        case _ @Rule.TypedEvaluated(_: Rule[T], _: Ignore)  => None
        case re @ Rule.TypedEvaluated(_: Rule[T], _: Deny)  => Some(Left(re.asInstanceOf[Eval[Deny]]))
        case re @ Rule.TypedEvaluated(_: Rule[T], _: Allow) => Some(Right(re.asInstanceOf[Eval[Allow]]))
      }
      .partitionMap(identity) match {
      case (_ @Nil, _ @Nil) => None
      case (_ @Nil, allows: List[Eval[Allow]]) =>
        Some(EngineEvalResult.EngineAllow(allows))
      case (denials: List[Eval[Deny]], _) =>
        Some(EngineEvalResult.EngineDeny(denials))
    }
  }
}
