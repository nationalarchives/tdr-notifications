package uk.gov.nationalarchives.notifications.decoders

object GenericMessageDecoder {
  case class GenericMessagesEvent(messages: List[GenericMessage]) extends IncomingEvent
  case class GenericMessage(message: String)
}
