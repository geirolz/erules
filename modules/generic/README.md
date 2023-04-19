# Erules Generic

**Sbt**
```sbt
  libraryDependencies += "com.github.geirolz" %% "erules-core" % "0.0.9"
  libraryDependencies += "com.github.geirolz" %% "erules-generic" % "0.0.9"
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