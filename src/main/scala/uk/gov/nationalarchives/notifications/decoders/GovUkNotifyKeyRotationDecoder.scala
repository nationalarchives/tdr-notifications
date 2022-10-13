package uk.gov.nationalarchives.notifications.decoders

object GovUkNotifyKeyRotationDecoder {
  case class Detail(`parameter-name`: String, `action-reason`: String)
  case class GovUkNotifyKeyRotationEvent(detail: Detail) extends IncomingEvent
}
