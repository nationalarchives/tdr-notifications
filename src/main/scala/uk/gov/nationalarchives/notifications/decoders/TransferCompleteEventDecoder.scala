package uk.gov.nationalarchives.notifications.decoders

object TransferCompleteEventDecoder {
  case class TransferCompleteEvent(
    transferringBodyName: String,
    consignmentReference: String,
    consignmentId: String,
    seriesName: String,
    userId: String,
    userEmail: String
  ) extends IncomingEvent
}
