package uk.gov.nationalarchives.notifications.decoders

object MetadataReviewRequestDecoder {
  case class MetadataReviewRequestEvent(
                                         transferringBodyName: String,
                                         consignmentReference: String,
                                         consignmentId: String,
                                         seriesCode: String,
                                         userId: String,
                                         userEmail: String
                                       ) extends IncomingEvent
}
