package uk.gov.nationalarchives.notifications.decoders

object MetadataDownloadDecoder {
  case class MetadataDownloadEvent(
      environment: String,
      userId: String,
      consignmentId: String,
      consignmentReference: String
  ) extends IncomingEvent
}
