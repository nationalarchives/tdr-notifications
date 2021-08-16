package uk.gov.nationalarchives.notifications.decoders

import io.circe.CursorOp.DownField
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.{Decoder, DecodingFailure, HCursor}

object KeycloakEventDecoder {
  case class KeycloakEvent(tdrEnv: String, message: String) extends IncomingEvent
  case class SNS(Message: String)
  case class Record(Sns: SNS)

  val decodeKeycloakEvent: Decoder[IncomingEvent] = (c: HCursor) => for {
    messages <- c.downField("Records").as[List[Record]]
    json <- parse(messages.head.Sns.Message)
      .left.map(e => DecodingFailure.fromThrowable(e, List(DownField("Message"))))
    event <- json.as[KeycloakEvent]
  } yield event
}
