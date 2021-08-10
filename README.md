# erules
[![Build Status](https://github.com/geirolz/erules/actions/workflows/cicd.yml/badge.svg)](https://github.com/geirolz/erules/actions)
[![codecov](https://img.shields.io/codecov/c/github/geirolz/erules)](https://codecov.io/gh/geirolz/erules)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/db3274b55e0c4031803afb45f58d4413)](https://www.codacy.com/manual/david.geirola/erules?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=geirolz/erules&amp;utm_campaign=Badge_Grade)
[![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/com.github.geirolz/erules-core_2.13?server=https%3A%2F%2Foss.sonatype.org)](https://mvnrepository.com/artifact/com.github.geirolz/erules-core)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![Mergify Status](https://img.shields.io/endpoint.svg?url=https://gh.mergify.io/badges/geirolz/erules&style=flat)](https://mergify.io)
[![GitHub license](https://img.shields.io/github/license/geirolz/erules)](https://github.com/geirolz/erules/blob/main/LICENSE)


A lightweight, simple, typed and functional rules engine evaluator using cats core.

## How to import

eRules supports Scala 2.13 and 3

**Sbt**
```
  libraryDependencies += "com.github.geirolz" %% "erules-core" % <version>
```


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

val checkCitizenship: Rule[Citizenship] =
  Rule("Check UK citizenship").check {
    case Citizenship(Country("UK")) => Allow.withoutReasons
    case _                          => Deny.because("Only UK citizenship is allowed!")
  }

val checkAdultAge: Rule[Age] =
  Rule("Check Age >= 18").check {
    case a: Age if a.value >= 18  => Allow.withoutReasons
    case _                        => Deny.because("Only >= 18 age are allowed!")
  }

val allPersonRules: NonEmptyList[Rule[Person]] = NonEmptyList.of(
  checkCitizenship
    .targetInfo("citizenship")
    .contramap(_.citizenship),
  checkAdultAge
    .targetInfo("age")
    .contramap(_.age)
)
```


Once we defied rules we just need to create the `RuleEngine` to evaluate that rules.

We can evaluate rules in two different ways:
- denyAllNotAllowed
- allowAllNotDenied

```scala
import erules.core.RulesEngine
import cats.effect.IO
import cats.effect.unsafe.implicits._

val person: Person = Person("Mimmo", "Rossi", Age(16), Citizenship(Country("IT")))

val result = for {
  engine <- RulesEngine.denyAllNotAllowed[IO, Person](allPersonRules)
  result <- engine.eval[IO](person)
} yield result

//yolo
result.unsafeRunSync().summary
```


Returns:
```text
###################### ENGINE VERDICT ######################

Data: Person(Mimmo,Rossi,Age(16),Citizenship(Country(IT)))
Rules: 2
Interpreter verdict: Denied

-------- Check UK citizenship for person.citizenship -------
- Rule: Check UK citizenship
- Description: 
- Target: person.citizenship
- Execution time: 61882 nanoseconds

- Verdict: Success(Deny)
- Because: Only UK citizenship is allowed!
------------------------------------------------------------
-------------- Check Age >= 18 for person.age --------------
- Rule: Check Age >= 18
- Description: 
- Target: person.age
- Execution time: 31984 nanoseconds

- Verdict: Success(Deny)
- Because: Only >= 18 age are allowed!
------------------------------------------------------------


############################################################
```