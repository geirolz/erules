package erules.cats.xml

import cats.xml.codec.Encoder
import cats.xml.XmlNode

import scala.concurrent.duration.FiniteDuration

private[xml] object GenericCatsXmlInstances extends GenericCatsXmlEncoderInstances
private[xml] sealed trait GenericCatsXmlEncoderInstances {

  import cats.xml.syntax.*

  implicit final val finiteDurationEncoder: Encoder[FiniteDuration] =
    Encoder.of(a =>
      XmlNode("Duration").withAttrs(
        "length" := a.length,
        "unit" := a.unit.name
      )
    )

  implicit def eitherEncoder[A, B](implicit a: Encoder[A], b: Encoder[B]): Encoder[Either[A, B]] =
    Encoder.of {
      case Left(v)  => v.toXml
      case Right(v) => v.toXml
    }

  implicit val throwableEncoder: Encoder[Throwable] =
    Encoder.of(ex =>
      XmlNode("Throwable").withAttrs(
        "message" := ex.getMessage,
        "causeMessage" := Option(ex.getCause).map(_.getMessage).getOrElse("")
      )
    )
}
