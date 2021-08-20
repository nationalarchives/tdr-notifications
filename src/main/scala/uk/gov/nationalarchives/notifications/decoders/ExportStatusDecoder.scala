package uk.gov.nationalarchives.notifications.decoders

import java.util.UUID

import io.circe.generic.auto._
import io.circe.{Decoder, HCursor}
import uk.gov.nationalarchives.notifications.decoders.IncomingEvent.{Record, parseSNSMessage}

object ExportStatusDecoder {
  case class ExportSuccessDetails(userId: UUID, consignmentReference: String, transferringBodyCode: String)
  case class ExportStatusEvent(
                                consignmentId: UUID,
                                success: Boolean,
                                environment: String,
                                successDetails: Option[ExportSuccessDetails],
                                failureCause: Option[String]) extends IncomingEvent
  val decodeExportStatusEvent: Decoder[IncomingEvent] = (c: HCursor) => for {
    messages <- c.downField("Records").as[List[Record]]
    json <- parseSNSMessage(messages.head.Sns.Message)
    status <- json.as[ExportStatusEvent]
  } yield status
}
