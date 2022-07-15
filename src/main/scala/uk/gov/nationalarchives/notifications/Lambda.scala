package uk.gov.nationalarchives.notifications

import java.io.{InputStream, OutputStream}

import cats.effect._
import cats.effect.unsafe.implicits.global
import io.circe.parser.decode
import uk.gov.nationalarchives.notifications.decoders.DiskSpaceAlarmDecoder.DiskSpaceAlarmEvent
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportStatusEvent
import uk.gov.nationalarchives.notifications.decoders.KeycloakEventDecoder.KeycloakEvent
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.ScanEvent
import uk.gov.nationalarchives.notifications.decoders.TransformEngineRetryDecoder.TransformEngineRetryEvent
import uk.gov.nationalarchives.notifications.decoders._
import uk.gov.nationalarchives.notifications.messages.EventMessages._
import uk.gov.nationalarchives.notifications.messages.Messages._

import scala.io.Source

class Lambda {
  def process(input: InputStream, output: OutputStream): String =
    IO.fromEither(decode[IncomingEvent](Source.fromInputStream(input).mkString).map {
      case scan: ScanEvent => sendMessages(scan)
      case exportStatus: ExportStatusEvent => sendMessages(exportStatus)
      case keycloakEvent: KeycloakEvent => sendMessages(keycloakEvent)
      case diskSpaceAlarmEvent: DiskSpaceAlarmEvent => sendMessages(diskSpaceAlarmEvent)
      case transformEngineRetryEvent: TransformEngineRetryEvent => sendMessages(transformEngineRetryEvent)
    }).flatten.unsafeRunSync()
  }
