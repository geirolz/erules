package erules.testing.scaltest

import cats.Functor
import erules.core.{EngineResult, RuleResult, RuleVerdict}
import org.scalatest.Assertion

trait ErulesAsyncAssertingSyntax {

  import cats.implicits.*

  implicit class RuleResultAssertingOps[F[_]: Functor, -T, +V <: RuleVerdict](
    fa: F[RuleResult[V]]
  ) {

    def assertingIgnoringTimes(f: RuleResult[V] => Assertion): F[Assertion] =
      fa.map(a => f(a.drainExecutionTime))
  }

  implicit class EngineResultAssertingOps[F[_]: Functor, T](fa: F[EngineResult[T]]) {

    def assertingIgnoringTimes(f: EngineResult[T] => Assertion): F[Assertion] =
      fa.map(a => f(a.drainExecutionsTime))
  }
}
