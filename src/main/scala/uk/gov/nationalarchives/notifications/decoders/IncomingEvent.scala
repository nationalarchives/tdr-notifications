package uk.gov.nationalarchives.notifications.decoders

import io.circe.CursorOp.DownField
import io.circe.parser.parse
import io.circe.{Decoder, DecodingFailure, Json}
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.decodeScanEvent
import uk.gov.nationalarchives.notifications.decoders.SSMMaintenanceDecoder.decodeMaintenanceEvent
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.decodeExportStatusEvent
import uk.gov.nationalarchives.notifications.decoders.KeycloakEventDecoder.decodeKeycloakEvent

trait IncomingEvent {
}

object IncomingEvent {
  implicit val allDecoders: Decoder[IncomingEvent] = decodeScanEvent or decodeMaintenanceEvent or decodeExportStatusEvent or
    decodeKeycloakEvent

  def parseSNSMessage(snsMessage: String): Either[DecodingFailure, Json] = {
    parse(snsMessage)
      .left.map(e => DecodingFailure.fromThrowable(e, List(DownField("Message"))))
  }

  case class SNS(Message: String)
  case class Record(Sns: SNS)
}
