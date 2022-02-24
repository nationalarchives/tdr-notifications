package uk.gov.nationalarchives.notifications.decoders

import uk.gov.nationalarchives.notifications.messages.EventMessages.ExportMessage

object TransformEngineRetryDecoder {
  case class TransformEngineRetryEvent(consignmentReference: String,
                                       retryCount: Int
                                      ) extends IncomingEvent with ExportMessage
}
