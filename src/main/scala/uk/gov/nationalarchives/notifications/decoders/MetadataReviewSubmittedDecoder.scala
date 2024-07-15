package uk.gov.nationalarchives.notifications.decoders

object MetadataReviewSubmittedDecoder {
  case class MetadataReviewSubmittedEvent(
                                           transferringBodyName: String,
                                           consignmentReference: String,
                                           consignmentId: String,
                                           userId: String,
                                           userEmail: String,
                                           urlLink: String
                                         ) extends IncomingEvent
}
