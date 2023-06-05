package erules

import org.scalacheck.Prop.forAll
import org.scalacheck.Test

class RuleRefSpec extends munit.ScalaCheckSuite {

  override def scalaCheckTestParameters: Test.Parameters =
    super.scalaCheckTestParameters
      .withMinSize(100)
      .withMinSuccessfulTests(2000)

  property("RuleRef.fromString is unique for each string") {
    val history: scala.collection.mutable.Map[String, RuleRef] = scala.collection.mutable.Map.empty
    forAll { (str: String) =>
      val ref = RuleRef.fromString(str)
      if (!history.contains(str))
        assert(!history.values.toList.contains(ref))
      else
        assertEquals(
          obtained = history(str),
          expected = ref
        )

      history.addOne((str, ref))
      assert(true)
    }
  }

  property("RuleRef.fromString return always the same RuleRef for the same string") {
    forAll { (str: String) =>
      assertEquals(
        obtained = RuleRef.fromString(str),
        expected = RuleRef.fromString(str)
      )
    }
  }
}
