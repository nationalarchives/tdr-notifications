package uk.gov.nationalarchives.notifications.decoders

import java.util.UUID

object BackendCheckFailureDecoder {
  case class BackendCheckFailureEvent(
                                       consignmentId: UUID,
                                       success: Boolean,
                                       environment: String,
                                       failureCause: String,
                                       backEndChecksProcess: String
                                     ) extends IncomingEvent
}

