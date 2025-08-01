package uk.gov.nationalarchives.notifications.decoders

object MetadataReviewSubmittedDecoder {
  case class MetadataReviewSubmittedEvent(
                                           environment: String,
                                           consignmentReference: String,
                                           urlLink: String,
                                           userEmail: String,
                                           status: String,
                                           transferringBodyName: String,
                                           seriesCode: String,
                                           userId: String,
                                           closedRecords: Boolean,
                                           totalRecords: Int
                                         ) extends IncomingEvent
}
