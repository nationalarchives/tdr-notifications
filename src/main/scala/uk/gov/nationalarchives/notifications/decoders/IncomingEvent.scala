package uk.gov.nationalarchives.notifications.decoders

import io.circe.Decoder
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.decodeScanEvent
import uk.gov.nationalarchives.notifications.decoders.SSMMaintenanceDecoder.decodeMaintenanceEvent
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.decodeExportStatusEvent

trait IncomingEvent {
}

object IncomingEvent {
  implicit val allDecoders: Decoder[IncomingEvent] = decodeScanEvent or decodeMaintenanceEvent or decodeExportStatusEvent
}
