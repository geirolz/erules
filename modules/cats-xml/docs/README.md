# Erules Cats Xml
The purpose of this module is to provide `Encoder` instances of `erules` types
and the `XmlReportEncoder` instances to produce an XML report.

**Sbt**
```sbt
  libraryDependencies += "com.github.geirolz" %% "erules-core" % "@VERSION@"
  libraryDependencies += "com.github.geirolz" %% "erules-cats-xml" % "@VERSION@"
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

val checkAdultAge: PureRule[Age] =
  Rule("Check Age >= 18") {
    case a: Age if a.value >= 18 => Allow.withoutReasons
    case _ => Deny.because("Only >= 18 age are allowed!")
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

Import
```scala mdoc:silent
import erules.cats.xml.implicits.*
```

Define the `Person` encoder
```scala mdoc:silent
import cats.xml.codec.Encoder
import cats.xml.XmlNode
import cats.xml.implicits.*
import scala.util.Try

implicit val personEncoder: Encoder[Person] = Encoder.of(person =>
  XmlNode("Person")
    .withAttrs(
      "name" := person.name,
      "lastName" := person.lastName,
      "age" := person.age.value
    )
    .withChildren(
      XmlNode("Citizenship")
        .withAttrs(
          "country" := person.citizenship.country.value
        )
    )
)
```

And create the XML report
```scala mdoc:to-string
import erules.*
import erules.implicits.*
import erules.cats.xml.implicits.*

val person: Person = Person("Mimmo", "Rossi", Age(16), Citizenship(Country("IT")))

val result: Try[EngineResult[Person]]  =
  RulesEngine
    .withRules(allPersonRules)
    .denyAllNotAllowed[Try]
    .map(_.seqEvalPure(person))

//yolo
result.get.asXmlReport
```