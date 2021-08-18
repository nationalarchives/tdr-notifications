package uk.gov.nationalarchives.notifications.decoders

import io.circe.generic.auto._
import io.circe.{Decoder, HCursor}
import uk.gov.nationalarchives.notifications.decoders.IncomingEvent.{Record, parseSNSMessage}

object KeycloakEventDecoder {
  case class KeycloakEvent(tdrEnv: String, message: String) extends IncomingEvent

  val decodeKeycloakEvent: Decoder[IncomingEvent] = (c: HCursor) => for {
    messages <- c.downField("Records").as[List[Record]]
    json <- parseSNSMessage(messages.head.Sns.Message)
    event <- json.as[KeycloakEvent]
  } yield event
}
