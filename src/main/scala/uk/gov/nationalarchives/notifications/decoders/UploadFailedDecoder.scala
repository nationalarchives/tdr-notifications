package uk.gov.nationalarchives.notifications.decoders

object UploadFailedDecoder {
  case class UploadFailedEvent(
      environment: String,
      consignmentReference: String,
      consignmentId: String,
      userId: String,
      isJudgmentUser: Boolean,
      errorMessage: Option[String]
  ) extends IncomingEvent
}
