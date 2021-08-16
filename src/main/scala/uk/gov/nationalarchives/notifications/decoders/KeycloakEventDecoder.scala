package uk.gov.nationalarchives.notifications.decoders

import io.circe.generic.auto._
import io.circe.{Decoder, HCursor}

object KeycloakEventDecoder {
  case class KeycloakEvent(message: String) extends IncomingEvent
  case class SNS(Message: String)
  case class Record(Sns: SNS)

  val decodeKeycloakEvent: Decoder[IncomingEvent] = (c: HCursor) => for {
    messages <- c.downField("Records").as[List[Record]]
  } yield KeycloakEvent(messages.head.Sns.Message)
}
