# Erules Generic

**Sbt**
```sbt
  libraryDependencies += "com.github.geirolz" %% "erules-core" % "@VERSION@"
  libraryDependencies += "com.github.geirolz" %% "erules-generic" % "@VERSION@"
```

### Usage

```scala
import cats.Id
import erules.Rule
import erules.PureRule
import erules.RuleVerdict
import erules.generic.implicits.*

case class Person(name: String, age: Int)

Rule.pure[Int]("Check age")
  .const(RuleVerdict.Allow.withoutReasons)
  .contramapTarget[Person](_.age)
```