package uk.gov.nationalarchives.notifications.decoders

object ParameterStoreExpiryEventDecoder {
  case class Detail(`parameter-name`: String, `action-reason`: String)
  case class ParameterStoreExpiryEvent(detail: Detail) extends IncomingEvent
}
