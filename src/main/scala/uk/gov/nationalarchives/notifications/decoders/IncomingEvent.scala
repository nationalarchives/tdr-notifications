package uk.gov.nationalarchives.notifications.decoders

import io.circe.CursorOp.DownField
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.{Decoder, DecodingFailure, HCursor, Json}
import uk.gov.nationalarchives.notifications.decoders.CloudwatchAlarmDecoder.CloudwatchAlarmEvent
import uk.gov.nationalarchives.notifications.decoders.DraftMetadataStepFunctionErrorDecoder.DraftMetadataStepFunctionError
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportStatusEvent
import uk.gov.nationalarchives.notifications.decoders.GenericMessageDecoder.GenericMessagesEvent
import uk.gov.nationalarchives.notifications.decoders.KeycloakEventDecoder.KeycloakEvent
import uk.gov.nationalarchives.notifications.decoders.MalwareScanThreatFoundEventDecoder.MalwareScanThreatFoundEvent
import uk.gov.nationalarchives.notifications.decoders.MetadataReviewRequestDecoder.MetadataReviewRequestEvent
import uk.gov.nationalarchives.notifications.decoders.MetadataReviewSubmittedDecoder.MetadataReviewSubmittedEvent
import uk.gov.nationalarchives.notifications.decoders.ParameterStoreExpiryEventDecoder.ParameterStoreExpiryEvent
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.decodeScanEvent
import uk.gov.nationalarchives.notifications.decoders.StepFunctionErrorDecoder.decodeStepFunctionError
import uk.gov.nationalarchives.notifications.decoders.TransferCompleteEventDecoder.TransferCompleteEvent
import uk.gov.nationalarchives.notifications.decoders.UploadEventDecoder.UploadEvent
import uk.gov.nationalarchives.notifications.decoders.UsersDisabledEventDecoder.UsersDisabledEvent

trait IncomingEvent {}

object IncomingEvent {
  implicit val allDecoders: Decoder[IncomingEvent] = decodeScanEvent or decodeSnsEvent[ExportStatusEvent] or
    decodeSnsEvent[KeycloakEvent] or decodeSnsEvent[GenericMessagesEvent] or
    decodeSnsEvent[CloudwatchAlarmEvent] or decodeSnsEvent[ParameterStoreExpiryEvent] or decodeStepFunctionError or decodeSnsEvent[TransferCompleteEvent] or
    decodeSnsEvent[MetadataReviewRequestEvent] or decodeSnsEvent[MetadataReviewSubmittedEvent] or decodeSnsEvent[DraftMetadataStepFunctionError] or decodeSnsEvent[MalwareScanThreatFoundEvent] or
    decodeSnsEvent[UploadEvent] or decodeSnsEvent[UsersDisabledEvent]

  def decodeSnsEvent[T <: IncomingEvent]()(implicit decoder: Decoder[T]): Decoder[IncomingEvent] = (c: HCursor) => for {
    messages <- c.downField("Records").as[List[SnsRecord]]
    json <- parseSNSMessage(messages.head.Sns.Message)
    event <- json.as[T]
  } yield event

  def parseSNSMessage(snsMessage: String): Either[DecodingFailure, Json] = {
    parse(snsMessage)
      .left.map(e => DecodingFailure.fromThrowable(e, List(DownField("Message"))))
  }

  case class SNS(Message: String)
  case class SnsRecord(Sns: SNS)
}
