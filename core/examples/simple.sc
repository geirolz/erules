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
import erules.*
import erules.RuleVerdict.*
import cats.data.NonEmptyList
import cats.Id

val checkCitizenship: PureRule[Citizenship] = Rule("Check UK citizenship") {
  case Citizenship(Country("UK")) => Allow.withoutReasons
  case _                          => Deny.because("Only UK citizenship is allowed!")
}

val checkAdultAge: PureRule[Age] = Rule("Check Age >= 18") {
  case a: Age if a.value >= 18 => Allow.withoutReasons
  case _                       => Deny.because("Only >= 18 age are allowed!")
}

val allPersonRules: NonEmptyList[PureRule[Person]] = NonEmptyList.of(
  checkCitizenship
    .targetInfo("citizenship")
    .contramap(_.citizenship),
  checkAdultAge
    .targetInfo("age")
    .contramap(_.age)
)

//-------------- RULES ENGINE --------------//
import erules.RulesEngine
import cats.effect.IO
import cats.effect.unsafe.implicits._

val person: Person = Person("Mimmo", "Rossi", Age(16), Citizenship(Country("IT")))

val result = for {
  engine <- RulesEngine
    .withRules[Id, Person](allPersonRules)
    .denyAllNotAllowed[IO]
  result = engine.seqEvalPure(person)
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
import erules.*
import erules.RuleVerdict.*
import cats.data.NonEmptyList

val checkCitizenship: PureRule[Citizenship] =
  Rule("Check UK citizenship") {
    case Citizenship(Country("UK")) => Allow.withoutReasons
    case _                          => Deny.because("Only UK citizenship is allowed!")
  }

val checkAdultAge: PureRule[Age] =
  Rule("Check Age >= 18") {
    case a: Age if a.value >= 18 => Allow.withoutReasons
    case _                       => Deny.because("Only >= 18 age are allowed!")
  }

val allPersonRules: NonEmptyList[PureRule[Person]] = NonEmptyList.of(
  checkCitizenship
    .targetInfo("person.citizenship")
    .contramap(_.citizenship),
  checkAdultAge
    .targetInfo("person.age")
    .contramap(_.age)
)

//-------------- RULES ENGINE --------------//
import erules.*
import cats.effect.IO
import cats.effect.unsafe.implicits._
import erules.implicits._

val person: Person = Person("Mimmo", "Rossi", Age(16), Citizenship(Country("IT")))

val result: IO[EngineResult[Person]] =
  RulesEngine
    .withRules(allPersonRules)
    .denyAllNotAllowed[IO]
    .map(_.seqEvalPure(person))

Console.println(result.unsafeRunSync().asReport)
