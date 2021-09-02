package uk.gov.nationalarchives.notifications.decoders

object KeycloakEventDecoder {
  case class KeycloakEvent(tdrEnv: String, message: String) extends IncomingEvent
}
