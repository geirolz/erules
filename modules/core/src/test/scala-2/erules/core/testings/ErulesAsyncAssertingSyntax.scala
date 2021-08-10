package erules.core.testings

import cats.Functor
import erules.core.{EngineResult, RuleResult, RuleVerdict}
import org.scalatest.matchers.should.Matchers
import org.scalatest.Assertion

trait ErulesAsyncAssertingSyntax { this: Matchers =>

  import cats.implicits.*

  implicit class RuleResultAssertingOps[F[_]: Functor, -T, +V <: RuleVerdict](fa: F[RuleResult[T, V]]) {

    def assertingIgnoringTimes(f: RuleResult[T, V] => Assertion): F[Assertion] =
      fa.map(a => f(a.drainExecutionTime))
  }

  implicit class EngineResultAssertingOps[F[_]: Functor, T](fa: F[EngineResult[T]]) {

    def assertingIgnoringTimes(f: EngineResult[T] => Assertion): F[Assertion] =
      fa.map(a => f(a.drainExecutionsTime))
  }
}
