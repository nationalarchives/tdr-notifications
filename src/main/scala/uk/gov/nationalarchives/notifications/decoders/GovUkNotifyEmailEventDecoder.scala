package uk.gov.nationalarchives.notifications.decoders

object GovUkNotifyEmailEventDecoder {
  case class GovUkNotifyEmailEvent(
                             templateId: String,
                             userEmail: String,
                             personalisation: Map[String, String],
                             reference: String) extends IncomingEvent
}
