package uk.gov.nationalarchives.notifications.decoders

import io.circe.{Decoder, HCursor}

import java.util.UUID

object DraftMetadataStepFunctionErrorDecoder {
  case class DraftMetadataStepFunctionError(consignmentId: UUID, metaDataError: String, cause: String, environment: String) extends IncomingEvent

  val decodeMetadataStepFunctionError: Decoder[IncomingEvent] = (c: HCursor) => for {
    metaDataError <- c.downField("metaDataError").as[String]
    cause <- c.downField("cause").as[String]
    environment <- c.downField("environment").as[String]
    consignmentId <- c.downField("consignmentId").as[UUID]
  } yield DraftMetadataStepFunctionError(consignmentId, metaDataError, cause, environment)
}
