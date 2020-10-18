package uk.gov.nationalarchives.scannotifications

import java.io.{InputStream, OutputStream}

import cats.FlatMap.ops.toAllFlatMapOps
import cats.effect._
import io.circe.parser.decode
import messages.Messages._
import uk.gov.nationalarchives.scannotifications.decoders.SSMMaintenanceDecoder.SSMMaintenanceEvent
import uk.gov.nationalarchives.scannotifications.decoders.ScanDecoder.ScanEvent
import uk.gov.nationalarchives.scannotifications.decoders._
import uk.gov.nationalarchives.scannotifications.messages.EventMessages._

import scala.io.Source

class Lambda {
  def process(input: InputStream, output: OutputStream): String =
    IO.fromEither(decode[IncomingEvent](Source.fromInputStream(input).mkString).map {
    case maintenance : SSMMaintenanceEvent => sendMessages(maintenance)
    case scan: ScanEvent => sendMessages(scan)
  }).flatten.unsafeRunSync()


}

