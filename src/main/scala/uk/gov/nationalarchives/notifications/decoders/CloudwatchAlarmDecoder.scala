package uk.gov.nationalarchives.notifications.decoders

object CloudwatchAlarmDecoder {

  case class AlarmDimension(name: String, `value`: String)
  case class AlarmTrigger(Dimensions: List[AlarmDimension], MetricName: String)
  case class CloudwatchAlarmEvent(NewStateValue: String, Trigger: AlarmTrigger, NewStateReason: String) extends IncomingEvent
}
