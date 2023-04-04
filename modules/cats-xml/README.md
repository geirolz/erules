# Erules Cats Xml
The purpose of this module is to provide `Encoder` instances of `erules` types
and the `XmlReportEncoder` instances to produce an XML report.

**Sbt**
```sbt
  libraryDependencies += "com.github.geirolz" %% "erules-core" % "0.0.8"
  libraryDependencies += "com.github.geirolz" %% "erules-cats-xml" % "0.0.8"
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
import erules.core.RuleVerdict.*
import cats.data.NonEmptyList
import cats.Id

val checkCitizenship: Rule[Id, Citizenship] =
  Rule("Check UK citizenship").apply[Id, Citizenship]{
    case Citizenship(Country("UK")) => Allow.withoutReasons
    case _                          => Deny.because("Only UK citizenship is allowed!")
  }
// checkCitizenship: Rule[Id, Citizenship] = RuleImpl(<function1>,RuleInfo(Check UK citizenship,None,None))

val checkAdultAge: Rule[Id, Age] =
  Rule("Check Age >= 18").apply[Id, Age] {
    case a: Age if a.value >= 18  => Allow.withoutReasons
    case _                        => Deny.because("Only >= 18 age are allowed!")
  }
// checkAdultAge: Rule[Id, Age] = RuleImpl(<function1>,RuleInfo(Check Age >= 18,None,None))

val allPersonRules: NonEmptyList[Rule[Id, Person]] = NonEmptyList.of(
  checkCitizenship
    .targetInfo("citizenship")
    .contramap(_.citizenship),
  checkAdultAge
    .targetInfo("age")
    .contramap(_.age)
)
// allPersonRules: NonEmptyList[Rule[Id, Person]] = NonEmptyList(RuleImpl(scala.Function1$$Lambda$11365/0x0000000802923a00@434a6b0a,RuleInfo(Check UK citizenship,None,Some(citizenship))), RuleImpl(scala.Function1$$Lambda$11365/0x0000000802923a00@1c4a6867,RuleInfo(Check Age >= 18,None,Some(age))))
```

Import
```scala
import erules.cats.xml.implicits.*
```

Define the `Person` encoder
```scala
import cats.xml.codec.Encoder
import cats.xml.XmlNode
import cats.xml.implicits.*

implicit val personEncoder: Encoder[Person] = Encoder.of(person =>
  XmlNode("Person")
    .withAttributes(
      "name" := person.name,
      "lastName" := person.lastName,
      "age" := person.age.value
    )
    .withChildren(
      XmlNode("Citizenship")
        .withAttributes(
          "country" := person.citizenship.country.value
        )
    )
)
```

And create the XML report
```scala
import erules.core.*
import erules.implicits.*
import erules.cats.xml.implicits.*

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
result.unsafeRunSync().asXmlReport
// res0: cats.xml.Xml = <EngineResult>
//  <Data>
//   <Person name="Mimmo" lastName="Rossi" age="16">
//    <Citizenship country="IT"/>
//   </Person>
//  </Data>
//  <Verdict type="Denied">
//   <EvaluatedRules>
//    <RuleResult>
//     <RuleInfo name="Check UK citizenship" description="" targetInfo="citizenship">
//      <FullDescription>
//       Check UK citizenship for citizenship
// </FullDescription>
//     </RuleInfo>
//     <Verdict type="Deny">
//      <Reasons>
//       <Reason>
//        Only UK citizenship is allowed!
// </Reason>
//      </Reasons>
//     </Verdict>
//     <ExecutionTime>
//      <Duration length="84708" unit="NANOSECONDS"/>
//     </ExecutionTime>
//    </RuleResult>
//    <RuleResult>
//     <RuleInfo name="Check Age >= 18" description="" targetInfo="age">
//      <FullDescription>Check Age >= 18 for age</FullDescription>
//     </RuleInfo>
//     <Verdict type="Deny">
//      <Reasons>
//       <Reason>Only >= 18 age are allowed!</Reason>
//      </Reasons>
//     </Verdict>
//     <ExecutionTime>
//      <Duration length="8000" unit="NANOSECONDS"/>
//     </ExecutionTime>
//    </RuleResult>
//   </EvaluatedRules>
//  </Verdict>
// </EngineResult>
```