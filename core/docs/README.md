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
  libraryDependencies += "com.github.geirolz" %% "erules-core" % "@VERSION@"
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

Assuming we want to check:
- The person is adult
- The person has a UK citizenship

Let's write the rules!
```scala mdoc:to-string
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

val checkAdultAge: PureRule[Age] =
  Rule("Check Age >= 18"){
    case a: Age if a.value >= 18  => Allow.withoutReasons
    case _                        => Deny.because("Only >= 18 age are allowed!")
  }

val allPersonRules: NonEmptyList[PureRule[Person]] = NonEmptyList.of(
  checkCitizenship
    .targetInfo("citizenship")
    .contramap(_.citizenship),
  checkAdultAge
    .targetInfo("age")
    .contramap(_.age)
)
```

N.B. Importing even the `erules-generic` you can use macro to auto-generate the target info using `contramapTarget` method.
`contramapTarget` apply contramap and derive the target info by the contramap parameter. The contramap parameter 
must be inline and have the following form: `_.bar.foo.test`.

Once we defied rules we just need to create the `RuleEngine` to evaluate that rules.

We can evaluate rules in two different ways:
- denyAllNotAllowed
- allowAllNotDenied

```scala mdoc:to-string
import erules.core.*
import erules.implicits.*
import cats.effect.IO
import cats.effect.unsafe.implicits.*

val person: Person = Person("Mimmo", "Rossi", Age(16), Citizenship(Country("IT")))

val result: IO[EngineResult[Person]]  = for {
  engine <- RulesEngine[IO].withRules[Id, Person](allPersonRules).denyAllNotAllowed
  result <- engine.parEval(person)
} yield result

//yolo
result.unsafeRunSync().asReport[String]
```


### Modules
- [erules-generic](https://github.com/geirolz/erules/tree/main/modules/generic)
- [erules-circe](https://github.com/geirolz/erules/tree/main/modules/circe)
- [erules-cats-xml](https://github.com/geirolz/erules/tree/main/modules/cats-xml)
- [erules-scalatest](https://github.com/geirolz/erules/tree/main/modules/scalatest)
