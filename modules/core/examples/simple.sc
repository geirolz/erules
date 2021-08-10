//----------------- DATA -----------------//
case class Country(value: String)
case class Age(value: Int)

case class Citizenship(country: Country)
case class Person(
 name: String,
 lastName: String,
 age: Age,
 citizenship: Citizenship
 )


//------------- CREATE RULES -------------//
import erules.core.Rule
import erules.core.EvalRuleResult._
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



//-------------- RULES ENGINE --------------//
import erules.core.RulesEngine
import cats.effect.IO
import cats.effect.unsafe.implicits._

val person: Person = Person("Mimmo", "Rossi", Age(16), Citizenship(Country("IT")))

val result = for {
  engine <- RulesEngine.denyAllNotAllowed[IO, Person](allPersonRules)
  result <- engine.parEval[IO](person)
} yield result

result.unsafeRunSync()

//----------------- DATA -----------------//
case class Country(value: String)
case class Age(value: Int)

case class Citizenship(country: Country)
case class Person(
 name: String,
 lastName: String,
 age: Age,
 citizenship: Citizenship
 )


//------------- CREATE RULES -------------//
import erules.core.Rule
import erules.core.EvalRuleResult._
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
    .targetInfo("person.citizenship")
    .contramap(_.citizenship),
  checkAdultAge
    .targetInfo("person.age")
    .contramap(_.age)
)



//-------------- RULES ENGINE --------------//
import erules.core.RulesEngine
import cats.effect.IO
import cats.effect.unsafe.implicits._

val person: Person = Person("Mimmo", "Rossi", Age(16), Citizenship(Country("IT")))

val result = for {
  engine <- RulesEngine.denyAllNotAllowed[IO, Person](allPersonRules)
  result <- engine.parEval[IO](person)
} yield result

result.unsafeRunSync()