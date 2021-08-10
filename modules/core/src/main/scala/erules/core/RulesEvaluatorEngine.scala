package erules.core

class RulesEvaluatorEngine[T] private (rules: List[Rule[T]], interpreter: EvalResultsInterpreter[T]) {

  def add(rule: Rule[T]): RulesEvaluatorEngine[T] =
    new RulesEvaluatorEngine(rules :+ rule, interpreter)

  def eval(data: T): EngineResult[T] = {

    val report = rules.map(rule => rule.evalZip(data))

    EngineResult(
      data = data,
      result = interpreter.interpret(report)
    )
  }
}
object RulesEvaluatorEngine {

  def apply[T](rules: Rule[T]*)(interpreter: EvalResultsInterpreter[T]): RulesEvaluatorEngine[T] =
    new RulesEvaluatorEngine[T](rules.toList, interpreter)

  def allowAllNotDenied[T](rules: Rule[T]*): RulesEvaluatorEngine[T] =
    RulesEvaluatorEngine(rules *)(EvalResultsInterpreter.Defaults.allowAllNotDenied[T])

  def denyAllNotAllowed[T](rules: Rule[T]*): RulesEvaluatorEngine[T] =
    RulesEvaluatorEngine(rules *)(EvalResultsInterpreter.Defaults.denyAllNotAllowed[T])
}
