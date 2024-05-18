# Erules
[![Build Status](https://github.com/geirolz/erules/actions/workflows/cicd.yml/badge.svg)](https://github.com/geirolz/erules/actions)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/db3274b55e0c4031803afb45f58d4413)](https://www.codacy.com/manual/david.geirola/erules?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=geirolz/erules&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/3c5de42e8bfd493d8a47478541118a4f)](https://app.codacy.com/gh/geirolz/erules/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)
[![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/com.github.geirolz/erules-core_2.13?server=https%3A%2F%2Foss.sonatype.org)](https://mvnrepository.com/artifact/com.github.geirolz/erules-core)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![Mergify Status](https://img.shields.io/endpoint.svg?url=https://api.mergify.com/v1/badges/geirolz/erules&style=flat)](https://mergify.io)
[![GitHub license](https://img.shields.io/github/license/geirolz/erules)](https://github.com/geirolz/erules/blob/main/LICENSE)

A lightweight, simple, typed, and functional rules engine evaluator using the Cats core.

## How to import

eRules supports Scala 2.13 and 3

**Sbt**
```sbt
  libraryDependencies += "com.github.geirolz" %% "erules-core" % "0.1.0"
```

---

## Glossary
- **Rule**: the definition of a rule, the *check* is pure and can be async. Each Rule must have a *description*. Each rule can have a *targetInfo* that is a string that describes the rule check target.
- **RuleVerdict**: Is the verdict of a rule, can be `Allow`, `Deny` or `Ignore`. Each kind of verdict can have 0 or more reasons.
- **RuleResult**: The rule result is just a case class to couple the `Rule` with its result `RuleVerdict` and some other information like the execution time.
- **EngineVerdict**: Same as `RuleVerdict` but related to the whole engine. Can be `Allowed` or `Denied`

---

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
- The person is an adult
- The person has UK citizenship

Let's write the rules!

Each Rule must have a unique name and can be:
- **Pure**: a pure function that takes a value and returns a `RuleVerdict`
- **Effect-ful**: a function that takes a value and returns a `F[RuleVerdict]` where `F` is a monad.

There are several ways to define a rule:
- **apply**: defines a complete rule from `T` to `F[RuleVerdict]` ( or `Id` for Pure Rules)
- **matchOrIgnore**: defines a partial function from `T` to `F[RuleVerdict]` ( or `Id` for Pure Rules). If the function is not defined for the input value, the rule is ignored.
- **const**: defines a rule that always returns the same `RuleVerdict` (e.g. `Allow` or `Deny`)
- **failed**: defines a rule that always fails with an exception
- **assert**: defines a rule from `T` to `F[Boolean]` ( or `Id` for Pure Rules) and returns `Allow` for `true` or `Deny` for `false`
- **assertNot**: defines a rule from `T` to `F[Boolean]` ( or `Id` for Pure Rules) and returns `Allow` for `false` or `Deny` for `true`
- **fromBooleanF**: defines a rule from `T` to `F[Boolean]` ( or `Id` for Pure Rules) where you can specify the behavior for `true` and `false` values.


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
// allPersonRules: NonEmptyList[PureRule[Person]] = NonEmptyList(RuleImpl(scala.Function1$$Lambda$12770/0x000000080343ed50@4f3549e4,RuleInfo(Check UK citizenship,None,Some(citizenship))), RuleImpl(scala.Function1$$Lambda$12770/0x000000080343ed50@5c9b046d,RuleInfo(Check Age >= 18,None,Some(age))))
```

---

N.B. Importing even the `erules-generic` you can use a macro to auto-generate the target info using the `contramapTarget` method. `contramapTarget` applies contramap and derives the target info by the contramap parameter. The contramap parameter must be inline and have the following form: `_.bar.foo.test`.

Once we define rules, we just need to create the `RuleEngine` to evaluate those rules.

We can run the engine in two ways:
- *denyAllNotAllowed*: to deny all is not explicitly allowed.
- *allowAllNotDenied*: to allow all is not explicitly denied.

Moreover, we can choose to run the engine in a pure way( with pure rules ) or in a monadic way (e.g. IO) using:
- *seqEvalPure*: to run the engine in a pure way with pure rules.
- *seqEval*: to sequentially run the engine in a monadic way.
- *parEval*: to parallel run the engine in a monadic way.
- *parEvalN*: to parallel run the engine in a monadic way with a fixed parallelism level.


```scala
import erules.*
import erules.implicits.*
import cats.effect.IO
import cats.effect.unsafe.implicits.*

val person: Person = Person("Mimmo", "Rossi", Age(16), Citizenship(Country("IT")))
// person: Person = Person(Mimmo,Rossi,Age(16),Citizenship(Country(IT)))

val result: IO[EngineResult[Person]] =
  RulesEngine
    .withRules[Id, Person](allPersonRules)
    .denyAllNotAllowed[IO]
    .map(_.seqEvalPure(person))
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
// - Execution time: *not measured*
// 
// - Verdict: Right(Deny)
// - Because: Only UK citizenship is allowed!
// ------------------------------------------------------------
// ------------------ Check Age >= 18 for age -----------------
// - Rule: Check Age >= 18
// - Description: 
// - Target: age
// - Execution time: *not measured*
// 
// - Verdict: Right(Deny)
// - Because: Only >= 18 age are allowed!
// ------------------------------------------------------------
// 
// 
// ############################################################
```

---

### Modules
- [erules-generic](https://github.com/geirolz/erules/tree/main/modules/generic)
- [erules-circe](https://github.com/geirolz/erules/tree/main/modules/circe)
- [erules-cats-xml](https://github.com/geirolz/erules/tree/main/modules/cats-xml)
- [erules-scalatest](https://github.com/geirolz/erules/tree/main/modules/scalatest)
