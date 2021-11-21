package erules.testing.scaltest

import org.scalatest.AsyncTestSuite

trait AsyncErulesSpec extends ErulesAsyncAssertingSyntax with ErulesMatchers with ReportValues {
  this: AsyncTestSuite =>
}
