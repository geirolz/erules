# Erules Cats Xml
The purpose of this module is to provide `Encoder` instances of `erules` types
and the `XmlReportEncoder` instances to produce an XML report.

**Sbt**
```sbt
  libraryDependencies += "com.github.geirolz" %% "erules-core" % "0.1.0"
  libraryDependencies += "com.github.geirolz" %% "erules-cats-xml" % "0.1.0"
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
// allPersonRules: NonEmptyList[PureRule[Person]] = NonEmptyList(RuleImpl(scala.Function1$$Lambda$11130/0x000000080284d2d8@51548dba,RuleInfo(Check UK citizenship,None,Some(citizenship))), RuleImpl(scala.Function1$$Lambda$11130/0x000000080284d2d8@1e2818b1,RuleInfo(Check Age >= 18,None,Some(age))))
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
import scala.util.Try

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
import erules.*
import erules.implicits.*
import erules.cats.xml.implicits.*

val person: Person = Person("Mimmo", "Rossi", Age(16), Citizenship(Country("IT")))
// person: Person = Person(Mimmo,Rossi,Age(16),Citizenship(Country(IT)))

val result: Try[EngineResult[Person]]  =
  RulesEngine
    .withRules(allPersonRules)
    .denyAllNotAllowed[Try]
    .map(_.seqEvalPure(person))
// result: Try[EngineResult[Person]] = Success(EngineResult(Person(Mimmo,Rossi,Age(16),Citizenship(Country(IT))),Denied(NonEmptyList(RuleResult(RuleInfo(Check UK citizenship,None,Some(citizenship)),Right(DenyImpl(List(EvalReason(Only UK citizenship is allowed!)))),None), RuleResult(RuleInfo(Check Age >= 18,None,Some(age)),Right(DenyImpl(List(EvalReason(Only >= 18 age are allowed!)))),None)))))

//yolo
result.get.asXmlReport
// res0: <none>.<root>.cats.xml.Xml = <EngineResult>
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
//    </RuleResult>
//   </EvaluatedRules>
//  </Verdict>
// </EngineResult>
```