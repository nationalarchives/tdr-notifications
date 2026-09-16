package uk.gov.nationalarchives.notifications.decoders

object MetadataDownloadDecoder {
  case class MetadataDownloadEvent(
      environment: String,
      userId: String,
      userName: String,
      consignmentId: String,
      consignmentReference: String
  ) extends IncomingEvent
}
