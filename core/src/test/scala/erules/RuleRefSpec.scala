package erules

import org.scalatest.funsuite.AnyFunSuite

class RuleRefSpec extends AnyFunSuite {

  test(""){

    //64
    val result: RuleRef = RuleRef.fromString("test")
    Console.println(result.value.bitLength)
  }
}
