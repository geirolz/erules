# Erules Circe
The purpose of this module is to provid `Encoder` instances of `erules` types
and the `JsonReportEncoder` instances to produce a json report.

**Sbt**
```sbt
  libraryDependencies += "com.github.geirolz" %% "erules-core" % "@VERSION@"
  libraryDependencies += "com.github.geirolz" %% "erules-circe" % "@VERSION@"
```

### Usage

Given these data classes
```scala mdoc:to-string
case class Country(value: String)
case class Age(value: Int)

case class Citizenship(country: Country)
case class Person(
  name: String,
  lastName: String,
  age: Age,
  citizenship: Citizenship
)
```

Let's write the rules!
```scala mdoc:to-string
import erules.core.Rule
import erules.core.RuleVerdict.*
import cats.data.NonEmptyList
import cats.Id

val checkCitizenship: Rule[Id, Citizenship] =
  Rule("Check UK citizenship").apply[Id, Citizenship]{
    case Citizenship(Country("UK")) => Allow.withoutReasons
    case _                          => Deny.because("Only UK citizenship is allowed!")
  }

val checkAdultAge: Rule[Id, Age] =
  Rule("Check Age >= 18").apply[Id, Age] {
    case a: Age if a.value >= 18  => Allow.withoutReasons
    case _                        => Deny.because("Only >= 18 age are allowed!")
  }

val allPersonRules: NonEmptyList[Rule[Id, Person]] = NonEmptyList.of(
  checkCitizenship
    .targetInfo("citizenship")
    .contramap(_.citizenship),
  checkAdultAge
    .targetInfo("age")
    .contramap(_.age)
)
```

Import 
```scala mdoc:silent
import erules.circe.implicits.*
```

And `circe-generic` to derive the `Person` encoder automatically
```scala mdoc:silent
import io.circe.generic.auto.*
```

And create the JSON report
```scala mdoc:to-string
import erules.core.*
import erules.implicits.*
import erules.circe.implicits.*

import cats.effect.IO
import cats.effect.unsafe.implicits.*

val person: Person = Person("Mimmo", "Rossi", Age(16), Citizenship(Country("IT")))

val result: IO[EngineResult[Person]]  = for {
  engine <- RulesEngine[IO].withRules[Id, Person](allPersonRules).denyAllNotAllowed
  result <- engine.parEval(person)
} yield result

//yolo
result.unsafeRunSync().asJsonReport
```