## How to test

### Scalatest
Using scalatest we can easily test our engine importing the `erules-scalatest` module.
```sbt mdoc
  libraryDependencies += "com.github.geirolz" %% "erules-scalatest" % "0.1.0"
```

#### Matchers
```scala
import erules.*
import erules.testing.scaltest.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import cats.effect.testing.scalatest.*

class MyTest extends AnyFunSuite
  with ErulesMatchers
  with Matchers {
  
  test("testing engine verdict - denied"){

    val verdict: RuleResultsInterpreterVerdict = ???

    verdict shouldBe denied
    verdict should not be allowed
  }

  test("testing rule verdict - allow"){

    val ruleVerdict: RuleVerdict = ???

    ruleVerdict shouldBe allow
    ruleVerdict should not be deny
    ruleVerdict should not be ignore
  }
}
```

#### Async effect

For async support we have to mix our test class with `AsyncErulesSpec`.
N.B. we are even using `AsyncIOSpec` from `cats-effect-testing-scalatest` library in order
to support cats `IO` monad.

#### Matchers
```scala
import erules.*
import erules.testing.scaltest.*
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import cats.effect.testing.scalatest.*
import cats.effect.IO

class MyTest extends AsyncFunSuite
  with AsyncErulesSpec
  with AsyncIOSpec
  with Matchers {

  test("testing rule result") {
    
    val rule: Rule[IO, String] = ???
    val result: IO[RuleResult.Unbiased] = rule.eval("FOO")
    
    result.assertingIgnoringTimes(
      _ shouldBe RuleResult.forRuleName("Allow all").succeeded(RuleVerdict.Allow.withoutReasons)
    )
  }
}
```