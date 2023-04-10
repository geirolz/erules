package erules.generic

import erules.core.Rule

import scala.annotation.{tailrec, unused}
import scala.reflect.macros.blackbox

private[generic] trait RuleMacros {
  implicit class RuleMacrosOps[F[_], T](@unused rule: Rule[F, T]) {

    /** Contramap `Rule` and add target info invoking `targetInfo` and passing the expression of the
      * map function `f`
      *
      * For example
      * {{{
      *   case class Foo(bar: Bar)
      *   case class Bar(test: Test)
      *   case class Test(value: Int)
      *
      *   val rule: Rule[F, Int]    = Rule("RULE").const(RuleVerdict.Ignore.withoutReasons)
      *   val fooRule: Rule[F, Foo] = rule.contramapTarget[Foo](_.bar.test.value)
      *
      *   fooRule.targetInfo
      *   scala> val res0: Option[String] = Some(bar.test.value)
      * }}}
      * @see
      *   [[Rule.contramap()]] and [[Rule.targetInfo()]] for further information
      */
    def contramapTarget[U](@unused path: U => T): Rule[F, U] =
      macro RuleImplMacros.contramapTarget[U, T]
  }
}
private[generic] object RuleMacros extends RuleMacros

private object RuleImplMacros {

  def contramapTarget[U: c.WeakTypeTag, T: c.WeakTypeTag](
    c: blackbox.Context
  )(path: c.Expr[U => T]): c.Tree = {
    import c.universe.*
    q"""{
      rule.contramap($path).targetInfo(${extractTargetInfoFromFunctionCall(c)(path)})
     }"""
  }

  private def extractTargetInfoFromFunctionCall[T: c.WeakTypeTag, U: c.WeakTypeTag](
    c: blackbox.Context
  )(
    path: c.Expr[T => U]
  ): c.Expr[String] = {
    import c.universe.*

    val expectedShapeInfo = "Path must have shape: _.field1.field2.field3.(...)"

    sealed trait PathElement
    case class TermPathElement(term: c.TermName, xargs: c.Tree*) extends PathElement
    case class FunctorPathElement(functor: c.Tree, method: c.TermName, xargs: c.Tree*)
        extends PathElement

    @tailrec
    def collectPathElements(tree: c.Tree, acc: List[PathElement]): List[PathElement] = {

      tree match {
        case q"$parent.$child " => collectPathElements(parent, TermPathElement(child) :: acc)
        case _: Ident           => acc
        case _ =>
          c.abort(c.enclosingPosition, s"Unsupported path element. $expectedShapeInfo, got: $tree")
      }
    }

    val pathEls = path.tree match {
      case q"($_) => $pathBody" => collectPathElements(pathBody, Nil)
      case _ => c.abort(c.enclosingPosition, s"$expectedShapeInfo, got: ${path.tree}")
    }

    c.Expr[String](q"${pathEls
        .collect {
          case TermPathElement(c)                => c.decodedName.toString
          case FunctorPathElement(_, method, _*) => method.decodedName.toString
        }
        .mkString(".")}")
  }
}
