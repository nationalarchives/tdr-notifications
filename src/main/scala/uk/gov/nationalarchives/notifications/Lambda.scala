package uk.gov.nationalarchives.notifications

import java.io.{InputStream, OutputStream}
import cats.effect._
import cats.effect.unsafe.implicits.global
import io.circe.parser.decode
import uk.gov.nationalarchives.notifications.decoders.CloudwatchAlarmDecoder.CloudwatchAlarmEvent
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportStatusEvent
import uk.gov.nationalarchives.notifications.decoders.GenericMessageDecoder.GenericMessagesEvent
import uk.gov.nationalarchives.notifications.decoders.KeycloakEventDecoder.KeycloakEvent
import uk.gov.nationalarchives.notifications.decoders.GovUkNotifyKeyRotationDecoder.GovUkNotifyKeyRotationEvent
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.ScanEvent
import uk.gov.nationalarchives.notifications.decoders.TransformEngineRetryDecoder.TransformEngineRetryEvent
import uk.gov.nationalarchives.notifications.decoders.TransformEngineV2RetryDecoder.TransformEngineV2RetryEvent
import uk.gov.nationalarchives.notifications.decoders._
import uk.gov.nationalarchives.notifications.messages.EventMessages._
import uk.gov.nationalarchives.notifications.messages.Messages._

import scala.io.Source

class Lambda {
  def process(input: InputStream, output: OutputStream): String = {
    val inputString = Source.fromInputStream(input).mkString
    IO.fromEither(decode[IncomingEvent](inputString).map {
      case scan: ScanEvent => sendMessages(scan)
      case exportStatus: ExportStatusEvent => sendMessages(exportStatus)
      case keycloakEvent: KeycloakEvent => sendMessages(keycloakEvent)
      case transformEngineRetryEvent: TransformEngineRetryEvent => sendMessages(transformEngineRetryEvent)
      case transformEngineV2RetryEvent: TransformEngineV2RetryEvent => sendMessages(transformEngineV2RetryEvent)
      case genericMessagesEvent: GenericMessagesEvent => sendMessages(genericMessagesEvent)
      case cloudwatchAlarmEvent: CloudwatchAlarmEvent => sendMessages(cloudwatchAlarmEvent)
      case govUkNotifyKeyRotationEvent: GovUkNotifyKeyRotationEvent => sendMessages(govUkNotifyKeyRotationEvent)
    }).flatten.unsafeRunSync()
  }
}
