package uk.gov.nationalarchives.notifications.decoders

import uk.gov.nationalarchives.notifications.messages.EventMessages.ExportMessage

object TransformEngineRetryDecoder {
  case class TransformEngineRetryEvent(consignmentReference: String,
                                       consignmentType: String,
                                       numberOfRetries: Int
                                      ) extends IncomingEvent with ExportMessage

}
