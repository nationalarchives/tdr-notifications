package uk.gov.nationalarchives.notifications.decoders

object UploadFailedDecoder {
  case class UploadFailedEvent(
      environment: String,
      consignmentReference: String,
      consignmentId: String,
      seriesName: Option[String],
      userId: String,
      errorMessage: Option[String]
  ) extends IncomingEvent
}
