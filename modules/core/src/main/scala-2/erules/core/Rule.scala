package erules.core

import cats.{Contravariant, Order, Show}
import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.*
import erules.core.RuleVerdict.Ignore

sealed trait Rule[-T] extends Serializable {

  /** A string to describe in summary this rule.
    */
  val name: String

  /** A string to add more information to this rule.
    */
  val description: Option[String]

  /** A string to describe what/who is the target of this rule.
    */
  val targetInfo: Option[String]

  // docs
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

  /** * Given a large class with nested case class for example. In order to test a single rule
    * without stud all useless data you may want to create a rule for the specific type and not for
    * the whole class.
    *
    * {{{
    *
    *   class Region(value: String) extends AnyVal
    *   class Citizenship(region: Region)
    *   class User(name: String, age: Int, citizenship: Citizenship)
    *
    *   val checkRegionIsUK: Rule[Region] = Rule("Check region is UK").check {
    *       case Region("UK") => Allow.withoutReasons
    *       case Region(value) => Deny.because(s"Only UK region is accepted! Actual value: $$value")
    *   }
    * }}}
    *
    * In this case if you want to apply this rule to a `User` instance con can use the `contramap`
    * method.
    *
    * {{{
    *   val checkUser: Rule[User] = checkRegionIsUK.contramap(_.citizenship.region)
    * }}}
    *
    * But dosing this if you want to keep the information that this rule doesn't check the whole
    * `User` instance but just a small sub-set of the data you can use `targetInfo` method to add
    * this information to this rule.
    *
    * The typical value of this method is "string" version of the contramap parameter.
    *
    * {{{
    *   val checkUser: Rule[User] = checkRegionIsUK
    *   .contramap(_.citizenship.region)
    *   .targetInfo("citizenship.region")
    * }}}
    *
    * NOTE: using `generic` module, with `import erules.generic.implicits.*` you can use
    * `contramapTarget` to both contramap and add target information.
    */
  def targetInfo(targetInfo: String): Rule[T]

  // map
  /** Contravariant version of the map.
    *
    * Given a large class with nested case class for example. In order to test a single rule without
    * stud all useless data you may want to create a rule for the specific type and not for the
    * whole class.
    *
    * {{{
    *
    *   class Region(value: String) extends AnyVal
    *   class Citizenship(region: Region)
    *   class User(name: String, age: Int, citizenship: Citizenship)
    *
    *   val checkRegionIsUK: Rule[Region] = Rule("Check region is UK").check {
    *       case Region("UK") => Allow.withoutReasons
    *       case Region(value) => Deny.because(s"Only UK region is accepted! Actual value: $$value")
    *   }
    * }}}
    *
    * In this case if you want to apply this rule to a `User` instance con can use the `contramap`
    * method.
    *
    * {{{
    *   val checkUser: Rule[User] = checkRegionIsUK.contramap(_.citizenship.region)
    * }}}
    *
    * NOTE: using `generic` module, with `import erules.generic.implicits.*` you can use
    * `contramapTarget` to both contramap and add target information.
    */
  def contramap[U](cf: U => T): Rule[U]

  // eval
  /** Same as `eval` but has only the `RuleVerdict` value
    */
  def evalRaw(data: T): IO[RuleVerdict]

  /** Eval this rules. The evaluations result is stored into a 'Try', so the `IO` doesn't raise
    * error in case of failed rule evaluation
    */
  final def eval(data: T): IO[RuleResult.Free[T]] =
    evalRaw(data).attempt.timed.map { case (duration, res) =>
      RuleResult(
        rule = this,
        res.toTry,
        executionTime = Some(duration)
      )
    }

  // std
  override final def equals(obj: Any): Boolean =
    obj != null && obj.isInstanceOf[Rule[T]] && this === obj.asInstanceOf[Rule[T]]
}
object Rule extends RuleInstances {

  import erules.core.utils.CollectionsUtils.*

  // =================/ BUILDER /=================
  def apply(name: String): RuleBuilder = new RuleBuilder(name)

  class RuleBuilder(name: String) { $this =>

    def asyncCheck[T](f: T => IO[RuleVerdict]): Rule[T] =
      RuleImpl(
        f           = f,
        name        = $this.name,
        description = None,
        targetInfo  = None
      )

    def asyncCheckOrIgnore[T](f: PartialFunction[T, IO[RuleVerdict]]): Rule[T] =
      asyncCheck[T](f.lift.andThen(_.getOrElse(Ignore.noMatch.pure[IO])))

    def check[T](f: T => RuleVerdict): Rule[T] =
      asyncCheck[T](f.andThen(IO.pure))

    def checkOrIgnore[T](f: PartialFunction[T, RuleVerdict]): Rule[T] =
      asyncCheckOrIgnore[T](f.andThen(t => IO.pure(t)))

    def const[T](v: RuleVerdict): Rule[T] =
      check[T](_ => v)

    def failed[T](ex: Throwable): Rule[T] =
      asyncCheck(_ => IO.raiseError(ex))

    private case class RuleImpl[-T](
      f: T => IO[RuleVerdict],
      name: String,
      description: Option[String] = None,
      targetInfo: Option[String]  = None
    ) extends Rule[T] {

      // docs
      def describe(description: String): Rule[T] =
        copy(description = Option(description))

      def targetInfo(targetInfo: String): Rule[T] =
        copy(targetInfo = Option(targetInfo))

      // map
      def contramap[U](cf: U => T): Rule[U] =
        copy(f = cf.andThen(f))

      // eval
      def evalRaw(data: T): IO[RuleVerdict] =
        f(data)
    }
  }

  // =================/ UTILS /=================
  def findDuplicated[T](rules: NonEmptyList[Rule[T]]): List[Rule[T]] =
    rules.findDuplicatedNem(identity)
}

private[erules] trait RuleInstances {

  implicit def catsContravariantForRule[T]: Contravariant[Rule] =
    new Contravariant[Rule] {
      override def contramap[A, B](fa: Rule[A])(f: B => A): Rule[B] = fa.contramap(f)
    }

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
