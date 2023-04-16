package erules.language

import cats.{Eq, Foldable, Order, Show}

sealed trait Expr[T] {

  def eval: T

  override final def toString: String = Show[Expr[T]].show(this)
}
object Expr extends ExprSyntax with ExprInstances {

  type BoolExpr = Expr[Boolean]

  val True: BoolExpr  = Val(true)
  val False: BoolExpr = Val(false)

  // construct
  final case class Val[T](eval: T) extends Expr[T]
  final case class If[T] private (expr: BoolExpr, ifTrue: Expr[T], ifFalse: Expr[T])
      extends Expr[T] {
    override def eval: T = if (expr.eval) ifTrue.eval else ifFalse.eval
  }
  object If {
    def apply[T](expr: BoolExpr)(ifTrue: Expr[T]): IfBuilder[T] =
      new IfBuilder(expr, ifTrue)

    class IfBuilder[T] private[If] (expr: BoolExpr, ifTrue: Expr[T]) {
      def Else(ifFalse: Expr[T]): If[T] =
        new If(expr, ifTrue, ifFalse)
    }
  }

  // conjunctions
  case class And[E1 <: BoolExpr, E2 <: BoolExpr](left: E1, right: E2) extends BoolExpr {
    def eval: Boolean = left.eval && right.eval
  }

  case class Or[E1 <: BoolExpr, E2 <: BoolExpr](left: E1, right: E2) extends BoolExpr {
    def eval: Boolean = left.eval || right.eval
  }

  case class Not[E <: BoolExpr](expr: E) extends BoolExpr {
    def eval: Boolean = !expr.eval
  }

  // primitives
  case class Equals[T: Eq](left: T, right: T) extends BoolExpr {
    def eval: Boolean = Eq[T].eqv(left, right)
  }
  case class LessThen[T: Order](left: T, right: T) extends BoolExpr {
    def eval: Boolean = Order[T].lt(left, right)
  }
  case class LessThenEq[T: Order](left: T, right: T) extends BoolExpr {
    def eval: Boolean = Order[T].lteqv(left, right)
  }
  case class GreaterThan[T: Order](left: T, right: T) extends BoolExpr {
    def eval: Boolean = Order[T].gt(left, right)
  }
  case class GreaterThanEq[T: Order](left: T, right: T) extends BoolExpr {
    def eval: Boolean = Order[T].gteqv(left, right)
  }

  // collections
  case class Contains[F[_]: Foldable, T: Eq](element: T, collection: F[T]) extends BoolExpr {
    def eval: Boolean = Foldable[F].contains_(collection, element)
  }
  case class IsEmpty[F[_]: Foldable, T](collection: F[T]) extends BoolExpr {
    def eval: Boolean = Foldable[F].isEmpty(collection)
  }
}
sealed trait ExprSyntax {

  import Expr.*

  def !![E <: BoolExpr](expr: E): Not[E] = Not(expr)

  implicit def anyToVal[T](value: T): Val[T] = Val(value)

  implicit class ExprOps[E1 <: BoolExpr](left: E1) {
    def &&[E2 <: BoolExpr](right: E2): And[E1, E2] = And(left, right)
    def ||[E2 <: BoolExpr](right: E2): Or[E1, E2]  = Or(left, right)
  }

  implicit class ExprValOps[T](a: T) {
    def ===(b: T)(implicit ev: Eq[T]): Equals[T]          = Equals(a, b)
    def !==(b: T)(implicit ev: Eq[T]): Not[Equals[T]]     = Not(Equals(a, b))
    def <(b: T)(implicit ev: Order[T]): LessThen[T]       = LessThen(a, b)
    def <=(b: T)(implicit ev: Order[T]): LessThenEq[T]    = LessThenEq(a, b)
    def >(b: T)(implicit ev: Order[T]): GreaterThan[T]    = GreaterThan(a, b)
    def >=(b: T)(implicit ev: Order[T]): GreaterThanEq[T] = GreaterThanEq(a, b)
  }

  implicit class ExprCollectionOps[I[_]: Foldable, T: Eq](fa: I[T]) {
    def contains(b: T): Expr.Contains[I, T] = Expr.Contains(b, fa)
    def isEmpty: Expr.IsEmpty[I, T]         = Expr.IsEmpty(fa)
  }
}

sealed trait ExprInstances {

  implicit def show[T]: Show[Expr[T]] = Show.show {
    case Expr.Val(value)                     => s"`$value`"
    case Expr.If(condition, ifTrue, ifFalse) => s"IF $condition THEN $ifTrue ELSE $ifFalse"
    case Expr.And(left, right)               => s"($left && $right)"
    case Expr.Or(left, right)                => s"($left || $right)"
    case Expr.Not(expr)                      => s"!($expr)"
    case Expr.Equals(left, right)            => s"$left == $right"
    case Expr.LessThen(left, right)          => s"$left < $right"
    case Expr.LessThenEq(left, right)        => s"$left <= $right"
    case Expr.GreaterThan(left, right)       => s"$left > $right"
    case Expr.GreaterThanEq(left, right)     => s"$left >= $right"
    case Expr.Contains(element, collection)  => s"$element IN [$collection]"
    case Expr.IsEmpty(collection)            => s"[$collection].isEmpty"
  }
}
