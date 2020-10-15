package uk.gov.nationalarchives.scannotifications

import java.io.{InputStream, OutputStream}
import cats.effect._
import io.circe.parser.decode
import uk.gov.nationalarchives.scannotifications.decoders.IncomingEvent._
import messages.Messages._
import uk.gov.nationalarchives.scannotifications.decoders.IncomingEvent

import scala.io.Source

class Lambda {
  def process(input: InputStream, output: OutputStream): String = (for {
    event <- IO.fromEither(decode[IncomingEvent](Source.fromInputStream(input).mkString))
    _ <- if (event.shouldSendEmail) event.sendEmailMessage else IO.unit
    _ <- if (event.shouldSendSlack) event.sendSlackMessage else IO.unit
  } yield "Notification event processed successfully").unsafeRunSync()


}

