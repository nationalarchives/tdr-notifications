package uk.gov.nationalarchives.notifications.decoders

import io.circe.{Decoder, HCursor}
import io.circe.generic.auto._
import uk.gov.nationalarchives.notifications.decoders.IncomingEvent.{Record, parseSNSMessage}

object DiskSpaceAlarmDecoder {

  case class AlarmDimension(name: String, `value`: String)
  case class AlarmTrigger(Dimensions: List[AlarmDimension], Threshold: Int)
  case class DiskSpaceAlarmEvent(NewStateValue: String, AlarmName: String, Trigger: AlarmTrigger) extends IncomingEvent

  val decodeDiskSpaceAlertEvent: Decoder[IncomingEvent] = (c: HCursor) => for {
    messages <- c.downField("Records").as[List[Record]]
    json <- parseSNSMessage(messages.head.Sns.Message)
    status <- json.as[DiskSpaceAlarmEvent]
  } yield status

}
