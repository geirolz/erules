package erules.core

import cats.{~>, Applicative, ApplicativeThrow, Contravariant, Functor, Id, Order, Show}
import cats.data.NonEmptyList
import cats.effect.Clock
import cats.implicits.*
import erules.core.RuleVerdict.Ignore

sealed trait Rule[F[_], -T] extends Serializable {

  /** Represent the rule info
    */
  val info: RuleInfo

  /** The unique rule reference
    */
  final lazy val uniqueRef: RuleRef = info.uniqueRef

  /** A string to describe in summary this rule.
    */
  final val name: String = info.name

  /** A string to add more information to this rule.
    */
  final val description: Option[String] = info.description

  /** A string to describe what/who is the target of this rule.
    */
  final val targetInfo: Option[String] = info.targetInfo

  /** A full description of the rule, that contains name, description and target info where defined.
    */
  final val fullDescription: String = info.fullDescription

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
    *   A lifted rule to specified effect type `G`
    */
  def covary[G[_]: Applicative](implicit env: F[RuleVerdict] <:< RuleVerdict): Rule[G, T]

  /** Lift a rule with effect `F[_]` to specified `G[_]`. Value is lifted using specified
    * `FunctionK` instance
    * @param f
    *   FunctionK instance to lift `F[_]` to `G[_]`
    * @tparam G
    *   new effect for the rule
    * @return
    *   A lifted rule to specified effect of type `G`
    */
  def mapK[G[_]](f: F ~> G): Rule[G, T]

  // eval
  /** Same as `eval` but has only the `RuleVerdict` value
    */
  def evalRaw[TT <: T](data: TT): F[RuleVerdict]

  /** Eval this rules. The evaluations result is stored into a 'Either[Throwable, T]', so the
    * `ApplicativeError` doesn't raise error in case of failed rule evaluation
    */
  final def eval[TT <: T](
    data: TT
  )(implicit F: ApplicativeThrow[F], C: Clock[F]): F[RuleResult.Unbiased] =
    C.timed(evalRaw[TT](data).attempt).map { case (duration, res) =>
      RuleResult.forRule(this)(
        verdict       = res,
        executionTime = Some(duration)
      )
    }

  // std
  override final def equals(obj: Any): Boolean =
    obj match {
      case that: Rule[_, _] =>
        Rule.catsOrderInstanceForRule[F, T].eqv(this, that.asInstanceOf[Rule[F, T]])
      case _ => false
    }
}

object Rule extends RuleInstances {

  import erules.core.utils.CollectionsUtils.*

  // =================/ BUILDER /=================
  def apply(name: String): RuleBuilder = new RuleBuilder(name)

  class RuleBuilder private[erules] (name: String) { $this =>

    def apply[F[_], T](f: Function[T, F[RuleVerdict]]): Rule[F, T] =
      check(f)

    def check[F[_], T](f: Function[T, F[RuleVerdict]]): Rule[F, T] =
      RuleImpl(
        f    = f,
        info = RuleInfo($this.name)
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

    // =================/ Pure /=================
    def apply[T](f: Function[T, RuleVerdict])(implicit dummyImplicit: DummyImplicit): PureRule[T] =
      apply[Id, T](f)

    def check[T](f: Function[T, RuleVerdict])(implicit dummyImplicit: DummyImplicit): PureRule[T] =
      check[Id, T](f)

    def partially[T](f: PartialFunction[T, RuleVerdict])(implicit
      dummyImplicit: DummyImplicit
    ): PureRule[T] =
      partially[Id, T](f)

    def const[T](v: RuleVerdict)(implicit dummyImplicit: DummyImplicit): PureRule[T] =
      const[Id, T](v)
  }

  private[erules] case class RuleImpl[F[_], -TT](
    f: TT => F[RuleVerdict],
    info: RuleInfo
  ) extends Rule[F, TT] {

    // docs
    override def describe(description: String): Rule[F, TT] =
      copy(info = info.copy(description = Option(description)))

    override def targetInfo(targetInfo: String): Rule[F, TT] =
      copy(info = info.copy(targetInfo = Option(targetInfo)))

    // map
    override def contramap[U](cf: U => TT): Rule[F, U] =
      copy(f = cf.andThen(f))

    // eval
    override def evalRaw[TT2 <: TT](data: TT2): F[RuleVerdict] =
      f(data)

    override def covary[G[_]: Applicative](implicit
      env: F[RuleVerdict] <:< RuleVerdict
    ): Rule[G, TT] =
      copy[G, TT](f = f.andThen(fa => Applicative[G].pure(env(fa))))

    override def mapK[G[_]](f: F ~> G): Rule[G, TT] =
      copy[G, TT](f = this.f.andThen(f.apply))
  }

  // =================/ UTILS /=================
  def findDuplicated[F[_], T](rules: NonEmptyList[Rule[F, T]]): List[Rule[F, T]] =
    rules.findDuplicatedNem(_.fullDescription)
}

private[erules] trait RuleInstances {

  implicit def catsContravariantForRule[F[_]]: Contravariant[Rule[F, *]] =
    new Contravariant[Rule[F, *]] {
      override def contramap[A, B](fa: Rule[F, A])(f: B => A): Rule[F, B] = fa.contramap(f)
    }

  implicit def catsOrderInstanceForRule[F[_], T]: Order[Rule[F, T]] =
    Order.from((x, y) =>
      if (
        x != null
        && y != null
        && x.info.eqv(y.info)
      ) 0
      else -1
    )

  implicit def catsShowInstanceForRule[F[_], T]: Show[Rule[F, T]] =
    r => s"Rule('${r.fullDescription}')"

  implicit class PureRuleOps[F[_]: Functor, I[X] <: Id[X], T](fa: F[Rule[I, T]]) {
    def mapLift[G[_]: Applicative]: F[Rule[G, T]] = fa.map(_.covary[G])
  }
}
