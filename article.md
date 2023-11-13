# Introduction to Erules Library: A Practical Guide

Erules is a lightweight, simple, and functional Scala library for rule evaluation. In this article, we will explore how to use this library to define and evaluate rules efficiently. Follow step-by-step for a detailed and hands-on understanding of use cases.

## What is Erules?

Erules is a library that provides a rule evaluation engine in Scala. It is designed to be straightforward, typed, and functional, leveraging the power of Cats core. With Erules, you can define rules clearly and evaluate them on various data types concisely.

## Step 1: Import the Library

To get started, add Erules as a dependency to your Scala project. You can do this in the `build.sbt` file with the following declaration:

```scala
libraryDependencies += "com.github.geirolz" %% "erules-core" % "0.1.0"
```

Make sure to check the [Maven page](https://mvnrepository.com/artifact/com.github.geirolz/erules-core) for the latest version.

## Step 2: Define Data Classes

Before defining rules, declare the data classes on which you want to apply them. For example:

```scala
case class Country(value: String)
case class Age(value: Int)

case class Citizenship(country: Country)
case class Person(name: String, lastName: String, age: Age, citizenship: Citizenship)
```

## Step 3: Write Rules

Now you can start defining the rules you want to apply to the data. 
Each Rule must have a unique name and can be:
- **Pure**: a pure function that takes a value and returns a `RuleVerdict`
- **Effectful**: a function that takes a value and returns a `F[RuleVerdict]` where `F` is a monad.

There are several ways to define a rule:
- **apply**: defines a complete rule from `T` to `F[RuleVerdict]` ( or `Id` for Pure Rules)
- **matchOrIgnore**: defines a partial function from `T` to `F[RuleVerdict]` ( or `Id` for Pure Rules). If the function is not defined for the input value, the rule is ignored.
- **const**: defines a rule that always returns the same `RuleVerdict` (e.g. `Allow` or `Deny`)
- **failed**: defines a rule that always fails with an exception
- **assert**: defines a rule from `T` to `F[Boolean]` ( or `Id` for Pure Rules) and returns `Allow` for `true` or `Deny` for `false`
- **assertNot**: defines a rule from `T` to `F[Boolean]` ( or `Id` for Pure Rules) and returns `Allow` for `false` or `Deny` for `true`
- **fromBooleanF**: defines a rule from `T` to `F[Boolean]` ( or `Id` for Pure Rules) where you can specify the behavior for `true` and `false` values.

For instance, let's say you want to check if a person is an adult and has UK citizenship:

```scala
import erules.Rule
import erules.PureRule
import erules.RuleVerdict.*
import cats.data.NonEmptyList

val checkCitizenship: PureRule[Citizenship] =
  Rule("Check UK citizenship") {
    case Citizenship(Country("UK")) => Allow.withoutReasons
    case _ => Deny.because("Only UK citizenship is allowed!")
  }

val checkAdultAge: PureRule[Age] =
  Rule("Check Age >= 18") {
    case a: Age if a.value >= 18 => Allow.withoutReasons
    case _ => Deny.because("Only >= 18 age are allowed!")
  }

val allPersonRules: NonEmptyList[PureRule[Person]] = NonEmptyList.of(
  checkCitizenship.targetInfo("citizenship").contramap(_.citizenship),
  checkAdultAge.targetInfo("age").contramap(_.age)
)
```

N.B. Importing even the `erules-generic` you can use a macro to auto-generate the target info using the `contramapTarget` method. 
`contramapTarget` applies contramap and derives the target info by the contramap parameter. 
The contramap parameter must be inline and have the following form: `_.bar.foo.test`.

## Step 4: Use the Evaluation Engine

Now that you've defined the rules, you can use the evaluation engine to check them.
You can run the engine in two ways:
- **denyAllNotAllowed**: to deny all is not explicitly allowed.
- **allowAllNotDenied**: to allow all is not explicitly denied.

Moreover, you can choose to run the engine in a pure way( with pure rules ) or in a monadic way (e.g. IO) using:
- **seqEvalPure**: to run the engine in a pure way with pure rules. 
- **seqEval**: to sequentially run the engine in a monadic way.
- **parEval**: to parallel run the engine in a monadic way.
- **parEvalN**: to parallel run the engine in a monadic way with a fixed parallelism level.

For example:
```scala
import erules.*
import erules.implicits.*
import cats.Id
import cats.effect.IO
import cats.effect.unsafe.implicits.*

val person: Person = Person("John", "Doe", Age(25), Citizenship(Country("UK")))

val result: IO[EngineResult[Person]] =
  RulesEngine
    .withRules[Id, Person](allPersonRules)
    .denyAllNotAllowed[IO]
    .map(_.seqEvalPure(person))

result.unsafeRunSync().asReport[String]
```

## Conclusion

Erules provides a powerful yet simple way to define and evaluate rules in Scala. By following these steps, you can integrate Erules into your projects for efficient and type-safe rule evaluation.

For more details, check out the [Erules GitHub Repository](https://github.com/geirolz/erules).
