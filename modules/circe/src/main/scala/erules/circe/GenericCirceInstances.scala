package erules.circe

import io.circe.{Encoder, Json}

import scala.concurrent.duration.FiniteDuration

private[circe] object GenericCirceInstances extends GenericCirceEncoderInstances
private[circe] sealed trait GenericCirceEncoderInstances {

  import io.circe.syntax.*

  implicit final val finiteDurationEncoder: Encoder[FiniteDuration] =
    Encoder.instance(a =>
      Json.obj(
        "length" -> Json.fromLong(a.length),
        "unit"   -> Json.fromString(a.unit.name)
      )
    )

  implicit def eitherEncoder[A, B](implicit a: Encoder[A], b: Encoder[B]): Encoder[Either[A, B]] =
    Encoder.instance {
      case Left(v)  => v.asJson
      case Right(v) => v.asJson
    }

  implicit val throwableEncoder: Encoder[Throwable] =
    Encoder.instance(ex =>
      Json.obj(
        "message"      -> Json.fromString(ex.getMessage),
        "causeMessage" -> Json.fromString(Option(ex.getCause).map(_.getMessage).getOrElse(""))
      )
    )
}
