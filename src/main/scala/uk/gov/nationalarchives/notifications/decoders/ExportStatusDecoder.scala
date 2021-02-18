package uk.gov.nationalarchives.notifications.decoders

import java.util.UUID

import io.circe.CursorOp.DownField
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.{Decoder, HCursor, _}

object ExportStatusDecoder {
  case class ExportOutputDetails(userId: UUID, consignmentReference: String, transferringBodyCode: String)
  case class ExportStatusEvent(consignmentId: UUID, success: Boolean, environment: String, exportOutput: Option[ExportOutputDetails]) extends IncomingEvent
  case class SNS(Message: String)
  case class Record(Sns: SNS)

  val decodeExportStatusEvent: Decoder[IncomingEvent] = (c: HCursor) => for {
    message <- c.downField("Records").as[List[Record]]
    json <- parse(message.head.Sns.Message)
      .left.map(e => DecodingFailure.fromThrowable(e, List(DownField("Message"))))
    status <- json.as[ExportStatusEvent]
  } yield status
}
