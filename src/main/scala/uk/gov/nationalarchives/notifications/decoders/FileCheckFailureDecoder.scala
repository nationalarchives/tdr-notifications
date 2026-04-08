package uk.gov.nationalarchives.notifications.decoders

import java.util.UUID

object FileCheckFailureDecoder {
  case class FileCheckFailureEvent(
                                    consignmentType: String,
                                    consignmentReference: String,
                                    consignmentId: UUID,
                                    transferringBodyName: String,
                                    userId: UUID,
                                    environment: String
                                  ) extends IncomingEvent {
    val isMockEvent: Boolean = transferringBodyName.toUpperCase.contains("MOCK")
  }
}

