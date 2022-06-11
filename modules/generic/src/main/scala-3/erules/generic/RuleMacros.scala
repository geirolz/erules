package erules.generic

import erules.core.Rule

import scala.annotation.tailrec

private[generic] trait RuleMacros:

  extension [F[_], T](rule: Rule[F, T])
    /** Contramap `Rule` and add target info invoking `targetInfo` and passing the expression of the
      * map function `f`
      *
      * For example
      * {{{
      *   case class Foo(bar: Bar)
      *   case class Bar(test: Test)
      *   case class Test(value: Int)
      *
      *   val rule: Rule[Int]    = Rule("RULE").const(RuleVerdict.Ignore.withoutReasons)
      *   val fooRule: Rule[Foo] = rule.contramapTarget[Foo](_.bar.test.value)
      *
      *   fooRule.targetInfo
      *   scala> val res0: Option[String] = Some(bar.test.value)
      * }}}
      *
      * @see
      *   [[Rule.contramap()]] and [[Rule.targetInfo()]] for further information
      */
    inline def contramapTarget[U](inline path: U => T): Rule[F, U] =
      ${ RuleImplMacros.contramapTargetImpl[F, U, T]('rule, 'path) }

private[generic] object RuleMacros extends RuleMacros

private object RuleImplMacros {

  import scala.quoted.*

  def contramapTargetImpl[F[_]: Type, U: Type, T: Type](
    rule: Expr[Rule[F, T]],
    path: Expr[U => T]
  )(using Quotes): Expr[Rule[F, U]] =
    '{
      $rule.contramap($path).targetInfo(${ extractTargetInfoFromFunctionCall(path) })
    }

  def extractTargetInfoFromFunctionCall[T: Type, U: Type](
    path: Expr[T => U]
  )(using Quotes): Expr[String] = {

    import quotes.reflect.*

    val expectedShapeInfo = "Path must have shape: _.field1.field2.each.field3.(...)"

    enum PathElement {
      case TermPathElement(term: String, xargs: String*) extends PathElement
      case FunctorPathElement(functor: String, method: String, xargs: String*) extends PathElement
    }

    def toPath(tree: Tree, acc: List[PathElement]): Seq[PathElement] = {
      tree match {
        /** Field access */
        case Select(deep, ident) =>
          toPath(deep, PathElement.TermPathElement(ident) :: acc)
        /** The first segment from path (e.g. `_.age` -> `_`) */
        case i: Ident =>
          acc
        case t =>
          report.errorAndAbort(s"Unsupported path element $t")
      }
    }

    val pathElements: Seq[PathElement] = path.asTerm match {
      /** Single inlined path */
      case Inlined(_, _, Block(List(DefDef(_, _, _, Some(p))), _)) =>
        toPath(p, List.empty)
      case _ =>
        report.errorAndAbort(s"Unsupported path [$path]")
    }

    Expr(
      pathElements
        .map {
          case PathElement.TermPathElement(c, _ @_*)            => c
          case PathElement.FunctorPathElement(_, method, _ @_*) => method
        }
        .mkString(".")
    )
  }
}
