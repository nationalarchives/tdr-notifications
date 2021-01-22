package uk.gov.nationalarchives.notifications.decoders

import java.util.UUID

import com.amazonaws.services.lambda.runtime.events.SNSEvent
import io.circe.CursorOp.DownField
import io.circe.{Decoder, HCursor}
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._

object ExportStatusDecoder {
  case class ExportStatusEvent(consignmentId: UUID, success: Boolean) extends IncomingEvent
  case class SNS(Message: String)
  case class Record(Sns: SNS)

  val decodeExportStatusEvent: Decoder[IncomingEvent] = (c: HCursor) => for {
    message <- c.downField("Records").as[List[Record]]
    json <- parse(message.head.Sns.Message)
      .left.map(e => DecodingFailure.fromThrowable(e, List(DownField("Message"))))
    status <- json.as[ExportStatusEvent]
  } yield status
}
