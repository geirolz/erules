package erules.testing.scaltest

import cats.effect.testing.scalatest.AssertingSyntax
import org.scalatest.AsyncTestSuite

trait AsyncErulesSpec extends ErulesAsyncAssertingSyntax { this: AsyncTestSuite & AssertingSyntax => }
