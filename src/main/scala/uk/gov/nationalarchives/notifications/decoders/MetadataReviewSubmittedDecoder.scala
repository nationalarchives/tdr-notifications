package uk.gov.nationalarchives.notifications.decoders

object MetadataReviewSubmittedDecoder {
  case class MetadataReviewSubmittedEvent(
                                           consignmentReference: String,
                                           urlLink: String,
                                           userEmail: String,
                                           status: String
                                         ) extends IncomingEvent
}
