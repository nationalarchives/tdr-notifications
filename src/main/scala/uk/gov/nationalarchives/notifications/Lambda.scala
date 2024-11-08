package uk.gov.nationalarchives.notifications

import java.io.{InputStream, OutputStream}
import cats.effect._
import cats.effect.unsafe.implicits.global
import io.circe.parser.decode
import uk.gov.nationalarchives.notifications.decoders.CloudwatchAlarmDecoder.CloudwatchAlarmEvent
import uk.gov.nationalarchives.notifications.decoders.DraftMetadataStepFunctionErrorDecoder.DraftMetadataStepFunctionError
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportStatusEvent
import uk.gov.nationalarchives.notifications.decoders.GenericMessageDecoder.GenericMessagesEvent
import uk.gov.nationalarchives.notifications.decoders.KeycloakEventDecoder.KeycloakEvent
import uk.gov.nationalarchives.notifications.decoders.MetadataReviewRequestDecoder.MetadataReviewRequestEvent
import uk.gov.nationalarchives.notifications.decoders.MetadataReviewSubmittedDecoder.MetadataReviewSubmittedEvent
import uk.gov.nationalarchives.notifications.decoders.ParameterStoreExpiryEventDecoder.ParameterStoreExpiryEvent
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.ScanEvent
import uk.gov.nationalarchives.notifications.decoders.StepFunctionErrorDecoder.StepFunctionError
import uk.gov.nationalarchives.notifications.decoders.TransferCompleteEventDecoder.TransferCompleteEvent
import uk.gov.nationalarchives.notifications.decoders._
import uk.gov.nationalarchives.notifications.messages.EventMessages._
import uk.gov.nationalarchives.notifications.messages.Messages._

import scala.io.Source

class Lambda {
  def process(input: InputStream, output: OutputStream): String = {
    val inputString = Source.fromInputStream(input).mkString
    IO.fromEither(decode[IncomingEvent](inputString).map {
      case scan: ScanEvent                                               => sendMessages(scan)
      case exportStatus: ExportStatusEvent                               => sendMessages(exportStatus)
      case keycloakEvent: KeycloakEvent                                  => sendMessages(keycloakEvent)
      case genericMessagesEvent: GenericMessagesEvent                    => sendMessages(genericMessagesEvent)
      case cloudwatchAlarmEvent: CloudwatchAlarmEvent                    => sendMessages(cloudwatchAlarmEvent)
      case parameterStoreExpiryEvent: ParameterStoreExpiryEvent          => sendMessages(parameterStoreExpiryEvent)
      case stepFunctionError: StepFunctionError                          => sendMessages(stepFunctionError)
      case transferCompleteEvent: TransferCompleteEvent                  => sendMessages(transferCompleteEvent)
      case metadataReviewRequestEvent: MetadataReviewRequestEvent        => sendMessages(metadataReviewRequestEvent)
      case metadataReviewSubmittedEvent: MetadataReviewSubmittedEvent    => sendMessages(metadataReviewSubmittedEvent)
      case draftMetadataStepFunctionEvent:DraftMetadataStepFunctionError => sendMessages(draftMetadataStepFunctionEvent)
    }).flatten
      .unsafeRunSync()
  }
}
