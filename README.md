# Erules
[![Build Status](https://github.com/geirolz/erules/actions/workflows/cicd.yml/badge.svg)](https://github.com/geirolz/erules/actions)
[![codecov](https://img.shields.io/codecov/c/github/geirolz/erules)](https://codecov.io/gh/geirolz/erules)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/db3274b55e0c4031803afb45f58d4413)](https://www.codacy.com/manual/david.geirola/erules?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=geirolz/erules&amp;utm_campaign=Badge_Grade)
[![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/com.github.geirolz/erules-core_2.13?server=https%3A%2F%2Foss.sonatype.org)](https://mvnrepository.com/artifact/com.github.geirolz/erules-core)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![Mergify Status](https://img.shields.io/endpoint.svg?url=https://api.mergify.com/v1/badges/geirolz/erules&style=flat)](https://mergify.io)
[![GitHub license](https://img.shields.io/github/license/geirolz/erules)](https://github.com/geirolz/erules/blob/main/LICENSE)


A lightweight, simple, typed and functional rules engine evaluator using cats core.

## How to import

eRules supports Scala 2.13 and 3

**Sbt**
```sbt
  libraryDependencies += "com.github.geirolz" %% "erules-core" % "0.0.3"
```



## Glossary
- **Rule** = the definition of a rule, the *check* is pure and can be async. 
Each Rule must have a *description*. Each rule can have a *targetInfo* that is a string
that describe the rule check target.
- **RuleVerdict** = Is the verdict of a rule, can be `Allow`, `Deny` or `Ignore`. Each kind of verdict can have 0 or more reasons.
- **RuleResult** = The rule result is just a case class to couple the `Rule` with is result `RuleVerdict` 
and some other information like the execution time.
- **EngineVerdict** = Same as `RuleVerdict` but related to the whole engine. Can be `Allowed` or `Denied`

## How to use

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

Assuming we want to check:
- The person is adult
- The person has a UK citizenship

Let's write the rules!
```scala
import erules.core.Rule
import erules.core.RuleVerdict.*
import cats.data.NonEmptyList
import cats.Id

val checkCitizenship: Rule[Id, Citizenship] =
  Rule("Check UK citizenship").apply[Id, Citizenship]{
    case Citizenship(Country("UK")) => Allow.withoutReasons
    case _                          => Deny.because("Only UK citizenship is allowed!")
  }
// checkCitizenship: Rule[Id, Citizenship] = RuleImpl(<function1>,Check UK citizenship,None,None)

val checkAdultAge: Rule[Id, Age] =
  Rule("Check Age >= 18").apply[Id, Age] {
    case a: Age if a.value >= 18  => Allow.withoutReasons
    case _                        => Deny.because("Only >= 18 age are allowed!")
  }
// checkAdultAge: Rule[Id, Age] = RuleImpl(<function1>,Check Age >= 18,None,None)

val allPersonRules: NonEmptyList[Rule[Id, Person]] = NonEmptyList.of(
  checkCitizenship
    .targetInfo("citizenship")
    .contramap(_.citizenship),
  checkAdultAge
    .targetInfo("age")
    .contramap(_.age)
)
// allPersonRules: NonEmptyList[Rule[Id, Person]] = NonEmptyList(RuleImpl(scala.Function1$$Lambda$13639/0x0000000803515040@67d92594,Check UK citizenship,None,Some(citizenship)), RuleImpl(scala.Function1$$Lambda$13639/0x0000000803515040@38c7f34f,Check Age >= 18,None,Some(age)))
```

N.B. Importing even the `erules-generic` you can use macro to auto-generate the target info using `contramapTarget` method.
`contramapTarget` apply contramap and derive the target info by the contramap parameter. The contramap parameter 
must be inline and have the following form: `_.bar.foo.test`.

Once we defied rules we just need to create the `RuleEngine` to evaluate that rules.

We can evaluate rules in two different ways:
- denyAllNotAllowed
- allowAllNotDenied

```scala
import erules.core.*
import erules.implicits.*
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
result.unsafeRunSync().asReport[String]
// res0: String = ###################### ENGINE VERDICT ######################
// 
// Data: Person(Mimmo,Rossi,Age(16),Citizenship(Country(IT)))
// Rules: 2
// Interpreter verdict: Denied
// 
// ------------ Check UK citizenship for citizenship -----------
// - Rule: Check UK citizenship
// - Description: 
// - Target: citizenship
// - Execution time: 144042 nanoseconds
// 
// - Verdict: Success(Deny)
// - Because: Only UK citizenship is allowed!
// ------------------------------------------------------------
// ------------------ Check Age >= 18 for age -----------------
// - Rule: Check Age >= 18
// - Description: 
// - Target: age
// - Execution time: 7917 nanoseconds
// 
// - Verdict: Success(Deny)
// - Because: Only >= 18 age are allowed!
// ------------------------------------------------------------
// 
// 
// ############################################################
```


### Modules
- [erules-generic](https://github.com/geirolz/erules/tree/main/modules/generic/src)
- [erules-scalatest](https://github.com/geirolz/erules/tree/main/modules/scalatest)
