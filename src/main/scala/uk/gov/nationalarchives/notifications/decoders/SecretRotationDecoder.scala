package uk.gov.nationalarchives.notifications.decoders

object SecretRotationDecoder {
  case class RotationResult(clientId: String, success: Boolean, rotationResultErrorMessage: Option[String] = None)
  case class RotationNotification(results: List[RotationResult]) extends IncomingEvent
}
