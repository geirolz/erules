# Erules Circe
The purpose of this module is to provide `Encoder` instances of `erules` types
and the `JsonReportEncoder` instances to produce a json report.

**Sbt**
```sbt
  libraryDependencies += "com.github.geirolz" %% "erules-core" % "0.0.9"
  libraryDependencies += "com.github.geirolz" %% "erules-circe" % "0.0.9"
```

### Usage

Given these data classes
```scala
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
```scala
import erules.core.Rule
import erules.core.PureRule
import erules.core.RuleVerdict.*
import cats.data.NonEmptyList
import cats.Id

val checkCitizenship: PureRule[Citizenship] =
  Rule("Check UK citizenship"){
    case Citizenship(Country("UK")) => Allow.withoutReasons
    case _                          => Deny.because("Only UK citizenship is allowed!")
  }
// checkCitizenship: Rule[Id, Citizenship] = RuleImpl(repl.MdocSession$MdocApp$$Lambda$56225/0x00000008094a42d0@7277a96c,RuleInfo(Check UK citizenship,None,None))

val checkAdultAge: PureRule[Age] =
  Rule("Check Age >= 18"){
    case a: Age if a.value >= 18  => Allow.withoutReasons
    case _                        => Deny.because("Only >= 18 age are allowed!")
  }
// checkAdultAge: Rule[Id, Age] = RuleImpl(repl.MdocSession$MdocApp$$Lambda$56229/0x00000008094bc2d0@40de50fc,RuleInfo(Check Age >= 18,None,None))

val allPersonRules: NonEmptyList[PureRule[Person]] = NonEmptyList.of(
  checkCitizenship
    .targetInfo("citizenship")
    .contramap(_.citizenship),
  checkAdultAge
    .targetInfo("age")
    .contramap(_.age)
)
// allPersonRules: NonEmptyList[PureRule[Person]] = NonEmptyList(RuleImpl(scala.Function1$$Lambda$21448/0x0000000804a1ede8@39a235e,RuleInfo(Check UK citizenship,None,Some(citizenship))), RuleImpl(scala.Function1$$Lambda$21448/0x0000000804a1ede8@2948a699,RuleInfo(Check Age >= 18,None,Some(age))))
```

Import 
```scala
import erules.circe.implicits.*
```

And `circe-generic` to derive the `Person` encoder automatically
```scala
import io.circe.generic.auto.*
```

And create the JSON report
```scala
import erules.core.*
import erules.implicits.*
import erules.circe.implicits.*

import cats.effect.IO
import cats.effect.unsafe.implicits.*

val person: Person = Person("Mimmo", "Rossi", Age(16), Citizenship(Country("IT")))
// person: Person = Person(Mimmo,Rossi,Age(16),Citizenship(Country(IT)))

val result: IO[EngineResult[Person]]  = for {
  engine <- RulesEngine[IO].withRules[Id, Person](allPersonRules).denyAllNotAllowed
  result <- engine.parEval(person)
} yield result
// result: IO[EngineResult[Person]] = IO(...)

//yolo
result.unsafeRunSync().asJsonReport
// res0: Json = {
//   "data" : {
//     "name" : "Mimmo",
//     "lastName" : "Rossi",
//     "age" : {
//       "value" : 16
//     },
//     "citizenship" : {
//       "country" : {
//         "value" : "IT"
//       }
//     }
//   },
//   "verdict" : {
//     "type" : "Denied",
//     "evaluatedRules" : [
//       {
//         "ruleInfo" : {
//           "name" : "Check UK citizenship",
//           "targetInfo" : "citizenship",
//           "fullDescription" : "Check UK citizenship for citizenship"
//         },
//         "verdict" : {
//           "type" : "Deny",
//           "reasons" : [
//             "Only UK citizenship is allowed!"
//           ]
//         },
//         "executionTime" : {
//           "length" : 87625,
//           "unit" : "NANOSECONDS"
//         }
//       },
//       {
//         "ruleInfo" : {
//           "name" : "Check Age >= 18",
//           "targetInfo" : "age",
//           "fullDescription" : "Check Age >= 18 for age"
//         },
//         "verdict" : {
//           "type" : "Deny",
//           "reasons" : [
//             "Only >= 18 age are allowed!"
//           ]
//         },
//         "executionTime" : {
//           "length" : 9042,
//           "unit" : "NANOSECONDS"
//         }
//       }
//     ]
//   }
// }
```