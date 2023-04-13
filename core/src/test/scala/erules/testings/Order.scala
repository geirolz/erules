package erules.testings

import cats.Show

case class Order(
  id: String,
  shipTo: ShipTo,
  billTo: BillTo,
  items: List[Item]
)
object Order {
  implicit val show: Show[Order] = Show.fromToString
}

case class ShipTo(
  address: String,
  country: Country
)

case class BillTo(
  address: String,
  country: Country
)

case class Item(
  id: String,
  quantity: Int,
  price: BigDecimal
)

case class Country(value: String) extends AnyVal
object Country {
  val IT: Country = Country("IT")
  val US: Country = Country("US")
  val UK: Country = Country("UK")
  val FR: Country = Country("FR")
}
