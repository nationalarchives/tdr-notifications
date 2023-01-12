package uk.gov.nationalarchives.notifications.decoders

import io.circe.{Decoder, HCursor}

import java.util.UUID

object StepFunctionErrorDecoder {
  case class StepFunctionError(consignmentId: UUID, error: String, cause: String, environment: String) extends IncomingEvent

  val decodeStepFunctionError: Decoder[IncomingEvent] = (c: HCursor) => for {
    error <- c.downField("error").as[String]
    cause <- c.downField("cause").as[String]
    environment <- c.downField("environment").as[String]
    consignmentId <- c.downField("consignmentId").as[UUID]
  } yield StepFunctionError(consignmentId, error, cause, environment)
}
