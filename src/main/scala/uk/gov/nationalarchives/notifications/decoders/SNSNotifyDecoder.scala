package uk.gov.nationalarchives.notifications.decoders

object SNSNotifyDecoder {
  case class Detail(`parameter-name`: String, `action-reason`: String)
  case class SNSNotifyEvent(detail: Detail) extends IncomingEvent
}
