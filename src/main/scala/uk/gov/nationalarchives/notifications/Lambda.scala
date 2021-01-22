package uk.gov.nationalarchives.notifications

import java.io.{InputStream, OutputStream}

import cats.FlatMap.ops.toAllFlatMapOps
import cats.effect._
import io.circe.parser.decode
import messages.Messages._
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportStatusEvent
import uk.gov.nationalarchives.notifications.decoders.SSMMaintenanceDecoder.SSMMaintenanceEvent
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.ScanEvent
import uk.gov.nationalarchives.notifications.decoders._
import uk.gov.nationalarchives.notifications.messages.EventMessages._

import scala.io.Source

class Lambda {
  def process(input: InputStream, output: OutputStream): String =
    IO.fromEither(decode[IncomingEvent](Source.fromInputStream(input).mkString).map {
    case maintenance : SSMMaintenanceEvent => sendMessages(maintenance)
    case scan: ScanEvent => sendMessages(scan)
    case exportStatus: ExportStatusEvent => sendMessages(exportStatus)
  }).flatten.unsafeRunSync()


}

