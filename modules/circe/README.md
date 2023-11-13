# Erules Circe
The purpose of this module is to provide `Encoder` instances of `erules` types
and the `JsonReportEncoder` instances to produce a json report.

**Sbt**
```sbt
  libraryDependencies += "com.github.geirolz" %% "erules-core" % "0.1.0"
  libraryDependencies += "com.github.geirolz" %% "erules-circe" % "0.1.0"
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
import erules.Rule
import erules.PureRule
import erules.RuleVerdict.*
import cats.data.NonEmptyList
import cats.Id

val checkCitizenship: PureRule[Citizenship] =
  Rule("Check UK citizenship") {
    case Citizenship(Country("UK")) => Allow.withoutReasons
    case _ => Deny.because("Only UK citizenship is allowed!")
  }
// checkCitizenship: PureRule[Citizenship] = RuleImpl(<function1>,RuleInfo(Check UK citizenship,None,None))

val checkAdultAge: PureRule[Age] =
  Rule("Check Age >= 18") {
    case a: Age if a.value >= 18 => Allow.withoutReasons
    case _ => Deny.because("Only >= 18 age are allowed!")
  }
// checkAdultAge: PureRule[Age] = RuleImpl(<function1>,RuleInfo(Check Age >= 18,None,None))

val allPersonRules: NonEmptyList[PureRule[Person]] = NonEmptyList.of(
  checkCitizenship
    .targetInfo("citizenship")
    .contramap(_.citizenship),
  checkAdultAge
    .targetInfo("age")
    .contramap(_.age)
)
// allPersonRules: NonEmptyList[PureRule[Person]] = NonEmptyList(RuleImpl(scala.Function1$$Lambda$12770/0x000000080343ed50@68efacd9,RuleInfo(Check UK citizenship,None,Some(citizenship))), RuleImpl(scala.Function1$$Lambda$12770/0x000000080343ed50@52ab816b,RuleInfo(Check Age >= 18,None,Some(age))))
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
import erules.*
import erules.implicits.*
import erules.circe.implicits.*
import scala.util.Try

val person: Person = Person("Mimmo", "Rossi", Age(16), Citizenship(Country("IT")))
// person: Person = Person(Mimmo,Rossi,Age(16),Citizenship(Country(IT)))

val result: Try[EngineResult[Person]] =
  RulesEngine
    .withRules(allPersonRules)
    .denyAllNotAllowed[Try]
    .map(_.seqEvalPure(person))
// result: Try[EngineResult[Person]] = Success(EngineResult(Person(Mimmo,Rossi,Age(16),Citizenship(Country(IT))),Denied(NonEmptyList(RuleResult(RuleInfo(Check UK citizenship,None,Some(citizenship)),Right(DenyImpl(List(EvalReason(Only UK citizenship is allowed!)))),None), RuleResult(RuleInfo(Check Age >= 18,None,Some(age)),Right(DenyImpl(List(EvalReason(Only >= 18 age are allowed!)))),None)))))

//yolo
result.get.asJsonReport
// res0: io.circe.Json = {
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
//         }
//       }
//     ]
//   }
// }
```