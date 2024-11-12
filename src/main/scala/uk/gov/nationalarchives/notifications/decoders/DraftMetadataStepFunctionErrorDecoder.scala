package uk.gov.nationalarchives.notifications.decoders

import io.circe.{Decoder, HCursor}

import java.util.UUID

object DraftMetadataStepFunctionErrorDecoder {
  case class DraftMetadataStepFunctionError(consignmentId: UUID, metaDataError: String, cause: String, environment: String) extends IncomingEvent
}
