package uk.gov.nationalarchives.notifications.decoders

object MetadataReviewSubmittedDecoder {
  case class MetadataReviewSubmittedEvent(
                                           consignmentReference: String,
                                           urlLink: String
                                         ) extends IncomingEvent
}
