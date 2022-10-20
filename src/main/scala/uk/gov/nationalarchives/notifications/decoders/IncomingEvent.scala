package uk.gov.nationalarchives.notifications.decoders

import cats.implicits.toFunctorOps
import io.circe.CursorOp.DownField
import io.circe.parser.parse
import io.circe.generic.auto._
import io.circe.{Decoder, DecodingFailure, HCursor, Json}
import uk.gov.nationalarchives.notifications.decoders.CloudwatchAlarmDecoder.CloudwatchAlarmEvent
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.decodeScanEvent
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportStatusEvent
import uk.gov.nationalarchives.notifications.decoders.KeycloakEventDecoder.KeycloakEvent
import uk.gov.nationalarchives.notifications.decoders.GenericMessageDecoder.GenericMessagesEvent
import uk.gov.nationalarchives.notifications.decoders.GovUkNotifyKeyRotationDecoder.GovUkNotifyKeyRotationEvent
import uk.gov.nationalarchives.notifications.decoders.TransformEngineRetryDecoder.TransformEngineRetryEvent
import uk.gov.nationalarchives.notifications.decoders.TransformEngineV2Decoder.{TransformEngineV2RetryEvent, UUIDs, TdrUUID, TreUUID}

trait IncomingEvent {
}

object IncomingEvent {
  implicit val uuidDecoder: Decoder[UUIDs] = Decoder[TdrUUID].widen or Decoder[TreUUID].widen
  implicit val allDecoders: Decoder[IncomingEvent] = decodeScanEvent or decodeSnsEvent[ExportStatusEvent] or
    decodeSnsEvent[KeycloakEvent] or decodeSqsEvent[TransformEngineRetryEvent] or decodeSnsEvent[GenericMessagesEvent] or
    decodeSnsEvent[CloudwatchAlarmEvent] or decodeSnsEvent[GovUkNotifyKeyRotationEvent] or decodeSqsEvent[TransformEngineV2RetryEvent]

  def decodeSnsEvent[T <: IncomingEvent]()(implicit decoder: Decoder[T]): Decoder[IncomingEvent] = (c: HCursor) => for {
    messages <- c.downField("Records").as[List[SnsRecord]]
    json <- parseSNSMessage(messages.head.Sns.Message)
    event <- json.as[T]
  } yield event

  def parseSNSMessage(snsMessage: String): Either[DecodingFailure, Json] = {
    parse(snsMessage)
      .left.map(e => DecodingFailure.fromThrowable(e, List(DownField("Message"))))
  }

  def decodeSqsEvent[T <: IncomingEvent]()(implicit decoder: Decoder[T]): Decoder[IncomingEvent] = (c: HCursor) => for {
    messages <- c.downField("Records").as[List[SqsRecord]]
    json <- parseSqsMessage(messages.head.body)
    event <- json.as[T]
  } yield event

  def parseSqsMessage(sqsRecord: String): Either[DecodingFailure, Json] = {
    parse(sqsRecord)
      .left.map(e => DecodingFailure.fromThrowable(e, List(DownField("Body"))))
  }

  case class SNS(Message: String)
  case class SnsRecord(Sns: SNS)
  case class SqsRecord(body: String)
}
