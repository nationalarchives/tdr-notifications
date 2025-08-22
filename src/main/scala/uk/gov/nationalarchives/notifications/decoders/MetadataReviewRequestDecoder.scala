package uk.gov.nationalarchives.notifications.decoders

object MetadataReviewRequestDecoder {
  case class MetadataReviewRequestEvent(
                                         environment: String,
                                         transferringBodyName: String,
                                         consignmentReference: String,
                                         consignmentId: String,
                                         seriesCode: String,
                                         userId: String,
                                         userEmail: String,
                                         closedRecords: Boolean,
                                         totalRecords: Int
                                       ) extends IncomingEvent {
    val isMockEvent:Boolean = transferringBodyName.toUpperCase.contains("MOCK")
  }
}