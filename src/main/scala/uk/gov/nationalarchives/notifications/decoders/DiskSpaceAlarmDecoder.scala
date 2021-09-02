package uk.gov.nationalarchives.notifications.decoders

object DiskSpaceAlarmDecoder {

  case class AlarmDimension(name: String, `value`: String)
  case class AlarmTrigger(Dimensions: List[AlarmDimension], Threshold: Int)
  case class DiskSpaceAlarmEvent(NewStateValue: String, AlarmName: String, Trigger: AlarmTrigger) extends IncomingEvent
}
