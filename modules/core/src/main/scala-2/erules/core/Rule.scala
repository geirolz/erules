package erules.core

import cats.{Eq, Order, Show}
import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.*
import erules.core.EvalRuleResult.{Deny, Ignore}
import erules.core.utils.Summarizable

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

sealed trait Rule[-T] extends Summarizable {

  /** A string to describe in summary this rule.
    */
  val name: String

  /** A string to add more information to this rule.
    */
  val description: Option[String]

  /** A string to describe what/who is the target of this rule.
    */
  val targetInfo: Option[String]

  //docs
  /** A full description of the rule, that contains name, description and target info where defined.
    */
  def fullDescription: String = {
    (description, targetInfo) match {
      case (None, None)                          => name
      case (Some(description), None)             => s"$name: $description"
      case (None, Some(targetInfo))              => s"$name for $targetInfo"
      case (Some(description), Some(targetInfo)) => s"$name for $targetInfo: $description"
    }
  }

  /** Set rule description
    */
  def describe(description: String): Rule[T]

  /** * Given a large class with nested case class for example. In order to test a single rule without stud all useless
    * data you may want to create a rule for the specific type and not for the whole class.
    *
    * {{{
    *
    *   class Region(value: String) extends AnyVal
    *   class Citizenship(region: Region)
    *   class User(name: String, age: Int, citizenship: Citizenship)
    *
    *   val checkRegionIsUK: Rule[Region] = Rule("Check region is UK").check {
    *       case Region("UK") => Allow.withoutReasons
    *       case Region(value) => Deny.because(s"Only UK region is accepted! Actual value: $value")
    *   }
    * }}}
    *
    * In this case if you want to apply this rule to a `User` instance con can use the `contramap` method.
    *
    * {{{
    *   val checkUser: Rule[User] = checkRegionIsUK.contramap(_.citizenship.region)
    * }}}
    *
    * But dosing this if you want to keep the information that this rule doesn't check the whole `User` instance but
    * just a small sub-set of the data you can use `targetInfo` method to add this information to this rule.
    *
    * The typical value of this method is "string" version of the contramap parameter.
    *
    * {{{
    *   val checkUser: Rule[User] = checkRegionIsUK
    *   .contramap(_.citizenship.region)
    *   .targetInfo("citizenship.region")
    * }}}
    */
  def targetInfo(targetInfo: String): Rule[T]

  /** A summary, human readable, for this instance
    * @inheritdoc
    */
  final override def summary: String =
    Show[Rule[T]].show(this)

  //map
  /** Contravariant version of the map.
    *
    * Given a large class with nested case class for example. In order to test a single rule without stud all useless
    * data you may want to create a rule for the specific type and not for the whole class.
    *
    * {{{
    *
    *   class Region(value: String) extends AnyVal
    *   class Citizenship(region: Region)
    *   class User(name: String, age: Int, citizenship: Citizenship)
    *
    *   val checkRegionIsUK: Rule[Region] = Rule("Check region is UK").check {
    *       case Region("UK") => Allow.withoutReasons
    *       case Region(value) => Deny.because(s"Only UK region is accepted! Actual value: $value")
    *   }
    * }}}
    *
    * In this case if you want to apply this rule to a `User` instance con can use the `contramap` method.
    *
    * {{{
    *   val checkUser: Rule[User] = checkRegionIsUK.contramap(_.citizenship.region)
    * }}}
    */
  def contramap[U](cmap: U => T): Rule[U]

  //eval
  def eval(data: T): IO[EvalRuleResult]

  /** Eval this rules. The evaluations result is stored into a [[Try]], so the `IO` doesn't raise error in case of
    * failed rule evaluation
    */
  final def evalZip(data: T): IO[Rule.Evaluated[T]] =
    eval(data).attempt
      .map(res => Rule.TypedEvaluated(this, res.toTry))

  /** Same as `evalZip` but timed using `IO.timed`.
    */
  final def timedEvalZip(data: T): IO[Rule.Evaluated[T]] =
    evalZip(data).timed.map { case (duration, evaluatedRule) =>
      evaluatedRule.copy(executionTime = Some(duration))
    }

  //std
  override final def equals(obj: Any): Boolean =
    obj != null && obj.isInstanceOf[Rule[T]] && this === obj.asInstanceOf[Rule[T]]
}
object Rule extends RuleInstances {

  import erules.core.utils.CollectionsUtils.*

  private[erules] val noMatch: Rule[Any] = Rule("No match").const(Ignore.noMatch)

  //=================/ BUILDER /=================
  def apply(name: String): RuleBuilder = new RuleBuilder(name)

  class RuleBuilder(name: String) { $this =>

    def asyncCheck[T](f: T => IO[EvalRuleResult]): Rule[T] =
      RuleImpl(
        f = f,
        name = $this.name,
        description = None,
        targetInfo = None
      )

    def asyncCheckOrIgnore[T](f: PartialFunction[T, IO[EvalRuleResult]]): Rule[T] =
      asyncCheck[T](f.lift.andThen(_.getOrElse(IO.pure(Ignore.noMatch))))

    def check[T](f: T => EvalRuleResult): Rule[T] =
      asyncCheck[T](f.andThen(IO.pure))

    def checkOrIgnore[T](f: PartialFunction[T, EvalRuleResult]): Rule[T] =
      asyncCheckOrIgnore[T](f.andThen(t => IO.pure(t)))

    def const[T](v: EvalRuleResult): Rule[T] =
      check[T](_ => v)

    private case class RuleImpl[-T](
      f: T => IO[EvalRuleResult],
      name: String,
      description: Option[String] = None,
      targetInfo: Option[String] = None
    ) extends Rule[T] {

      //docs
      def describe(description: String): Rule[T] =
        copy(description = Option(description))

      def targetInfo(targetInfo: String): Rule[T] =
        copy(targetInfo = Option(targetInfo))

      //map
      def contramap[U](cmap: U => T): Rule[U] =
        copy(f = cmap.andThen(f))

      //eval
      def eval(data: T): IO[EvalRuleResult] =
        f(data)
    }
  }

  //=================/ EVALUATED /=================
  type Evaluated[-T] = TypedEvaluated[T, EvalRuleResult]
  case class TypedEvaluated[-T, +R <: EvalRuleResult](
    rule: Rule[T],
    result: Try[R],
    executionTime: Option[FiniteDuration] = None
  ) extends Summarizable {
    override def summary: String = Show[TypedEvaluated[T, ? <: EvalRuleResult]].show(this)
  }
  object TypedEvaluated {
    def noMatch[R <: EvalRuleResult](v: R): TypedEvaluated[Any, R] =
      TypedEvaluated(Rule.noMatch, Success(v))

    def denyForSafetyInCaseOfError[T](rule: Rule[T], ex: Throwable): TypedEvaluated[T, Deny] =
      TypedEvaluated(rule, Failure(ex))

    implicit def catsOrderInstanceForRuleTypedEvaluated[T, R <: EvalRuleResult](implicit
      ruleEq: Eq[Rule[T]]
    ): Order[Rule.TypedEvaluated[T, R]] =
      Order.from((x, y) =>
        if (
          x != null
          && y != null
          && ruleEq.eqv(x.rule, y.rule)
          && x.result.equals(y.result)
          && x.executionTime.equals(y.executionTime)
        ) 0
        else -1
      )

    implicit def catsShowInstanceForRuleTypedEvaluated[T]: Show[Rule.TypedEvaluated[T, ? <: EvalRuleResult]] =
      er => {

        val reasons: String = er.result.map(_.reasons) match {
          case Failure(ex)      => s"- Failed: $ex"
          case Success(Nil)     => ""
          case Success(reasons) => s"- Because: ${EvalReason.stringifyList(reasons)}"
        }

        s"""|- Rule: ${er.rule.name}
            |- Description: ${er.rule.description.getOrElse("")}
            |- Target: ${er.rule.targetInfo.getOrElse("")}
            |- Execution time: ${er.executionTime.map(Show.catsShowForFiniteDuration.show).getOrElse("*not measured*")}
            |
            |- Result: ${er.result.map(_.typeName)}
            |$reasons""".stripMargin
      }
  }

  //=================/ UTILS /=================
  def findDuplicated[T](rules: NonEmptyList[Rule[T]]): List[Rule[T]] =
    rules.findDuplicatedNem(identity)
}

private[erules] trait RuleInstances {

  implicit def catsOrderInstanceForRule[T]: Order[Rule[T]] =
    Order.from((x, y) =>
      if (
        x != null
        && y != null
        && x.name.equals(y.name)
        && x.targetInfo.equals(y.targetInfo)
        && x.description.equals(y.description)
      ) 0
      else -1
    )

  implicit def catsShowInstanceForRule[T]: Show[Rule[T]] =
    r => s"Rule('${r.fullDescription}')"
}
