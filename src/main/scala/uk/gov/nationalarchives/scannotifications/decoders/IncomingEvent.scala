package uk.gov.nationalarchives.scannotifications.decoders

import io.circe.Decoder
import uk.gov.nationalarchives.scannotifications.decoders.ScanDecoder.decodeScanEvent
import uk.gov.nationalarchives.scannotifications.decoders.SSMMaintenanceDecoder.decodeMaintenanceEvent

trait IncomingEvent {
}

object IncomingEvent {
  implicit val allDecoders: Decoder[IncomingEvent] = decodeScanEvent or decodeMaintenanceEvent
}
