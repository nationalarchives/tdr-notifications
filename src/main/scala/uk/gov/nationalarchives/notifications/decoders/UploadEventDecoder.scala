package uk.gov.nationalarchives.notifications.decoders

object UploadEventDecoder {
  case class UploadEvent(
    transferringBodyName: String,
    consignmentReference: String,
    consignmentId: String,
    status: String,
    userId: String,
    userEmail: String,
    uploadSource: String,
    environment: String
  ) extends IncomingEvent
}
