package uk.gov.nationalarchives.notifications.decoders

object UploadEventDecoder {
  case class UploadEvent(
                          transferringBodyName: String,
                          consignmentReference: String,
                          consignmentId: String,
                          status: String,
                          userId: String,
                          userEmail: String,
                          assetSource: String,
                          environment: String
                        ) extends IncomingEvent {
    val isMockEvent: Boolean = transferringBodyName.toUpperCase.contains("MOCK")
  }
}
