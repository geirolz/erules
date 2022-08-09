package erules.core

import cats.{Applicative, ApplicativeThrow, Contravariant, Eq, Functor, Order, Show}
import cats.data.NonEmptyList
import cats.effect.Clock
import cats.implicits.*
import erules.core.Rule.RuleBuilder
import erules.core.RuleVerdict.Ignore

import scala.util.Try

sealed trait Rule[+F[_], -T] extends Serializable {

  /** A string to describe in summary this rule.
    */
  val name: String

  /** A string to add more information to this rule.
    */
  val description: Option[String]

  /** A string to describe what/who is the target of this rule.
    */
  val targetInfo: Option[String]

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
  def describe(description: String): Rule[F, T]

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
  def targetInfo(targetInfo: String): Rule[F, T]

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
  def contramap[U](cf: U => T): Rule[F, U]

  /** Lift a pure, side-effect free rule with effect `Id[_]` to specified `G[_]`. Value is lifted as
    * a pure effect using `Applicative`
    * @tparam G
    *   Effect
    * @return
    *   A lifted rule to specifed effect type `G`
    */
  def covary[G[_]: Applicative](implicit env: F[RuleVerdict] <:< RuleVerdict): Rule[G, T]

  // eval
  /** Same as `eval` but has only the `RuleVerdict` value
    */
  def evalRaw[FF[X] >: F[X], TT <: T](data: TT): FF[RuleVerdict]

  /** Eval this rules. The evaluations result is stored into a 'Either[Throwable, T]', so the
    * `ApplicativeError` doesn't raise error in case of failed rule evaluation
    */
  final def eval[FF[X] >: F[X], TT <: T](
    data: TT
  )(implicit F: ApplicativeThrow[FF], C: Clock[FF]): FF[RuleResult.Free[TT]] =
    C.timed(
      evalRaw[FF, TT](data).attempt
    ).map { case (duration, res) =>
      RuleResult[TT, RuleVerdict](
        rule          = this,
        verdict       = res,
        executionTime = Some(duration)
      )
    }

  // std
  override final def equals(obj: Any): Boolean =
    Try(obj.asInstanceOf[Rule[F, T]])
      .map(Eq[Rule[F, T]].eqv(this, _))
      .getOrElse(false)
}

object Rule extends RuleInstances with RuleSyntax {

  import erules.core.utils.CollectionsUtils.*

  // =================/ BUILDER /=================
  def apply(name: String): RuleBuilder = new RuleBuilder(name)

  class RuleBuilder private[erules] (name: String) { $this =>

    def apply[F[_], T](f: Function[T, F[RuleVerdict]]): Rule[F, T] =
      check(f)

    def check[F[_], T](f: Function[T, F[RuleVerdict]]): Rule[F, T] =
      RuleImpl(
        f           = f,
        name        = $this.name,
        description = None,
        targetInfo  = None
      )

    def partially[F[_]: Applicative, T](
      f: PartialFunction[T, F[RuleVerdict]]
    ): Rule[F, T] =
      apply(
        f.lift.andThen(_.getOrElse(Applicative[F].pure(Ignore.noMatch)))
      )

    def failed[F[_]: ApplicativeThrow, T](ex: Throwable): Rule[F, T] =
      apply(_ => ApplicativeThrow[F].raiseError(ex))

    def const[F[_]: Applicative, T](v: RuleVerdict): Rule[F, T] =
      apply(_ => Applicative[F].pure(v))
  }

  private[erules] case class RuleImpl[+F[_], -TT](
    f: TT => F[RuleVerdict],
    name: String,
    description: Option[String] = None,
    targetInfo: Option[String]  = None
  ) extends Rule[F, TT] {

    // docs
    def describe(description: String): Rule[F, TT] =
      copy(description = Option(description))

    def targetInfo(targetInfo: String): Rule[F, TT] =
      copy(targetInfo = Option(targetInfo))

    // map
    def contramap[U](cf: U => TT): Rule[F, U] =
      copy(f = cf.andThen(f))

    // eval
    def evalRaw[FF[X] >: F[X], TT2 <: TT](data: TT2): FF[RuleVerdict] =
      f(data)

    def covary[G[_]: Applicative](implicit
      env: F[RuleVerdict] <:< RuleVerdict
    ): Rule[G, TT] =
      copy[G, TT](f = f.andThen(fa => Applicative[G].pure(env(fa))))
  }

  // =================/ UTILS /=================
  def findDuplicated[F[_], T](rules: NonEmptyList[Rule[F, T]]): List[Rule[F, T]] =
    rules.findDuplicatedNem(identity)
}

private[erules] trait RuleInstances {

  implicit def catsContravariantForRule[F[_], T]: Contravariant[Rule[F, *]] =
    new Contravariant[Rule[F, *]] {
      override def contramap[A, B](fa: Rule[F, A])(f: B => A): Rule[F, B] = fa.contramap(f)
    }

  implicit def catsOrderInstanceForRule[F[_], T]: Order[Rule[F, T]] =
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

  implicit def catsShowInstanceForRule[F[_], T]: Show[Rule[F, T]] =
    r => s"Rule('${r.fullDescription}')"

  implicit class PureRuleOps[F[_]: Functor, T](fa: F[PureRule[T]]) {
    def mapLift[G[_]: Applicative]: F[Rule[G, T]] = fa.map(_.covary[G])
  }
}

private[erules] trait RuleSyntax {
  implicit class RuleBuilderStringOps(private val ctx: StringContext) {
    def r(args: Any*): RuleBuilder = new RuleBuilder(ctx.s(args))
  }
}
