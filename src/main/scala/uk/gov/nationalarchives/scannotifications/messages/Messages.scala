package uk.gov.nationalarchives.scannotifications.messages

import cats.effect.{ContextShift, IO}
import com.typesafe.config.ConfigFactory
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.{basicRequest, _}
import sttp.model.MediaType
import uk.gov.nationalarchives.aws.utils.Clients.ses
import uk.gov.nationalarchives.aws.utils.SESUtils
import uk.gov.nationalarchives.aws.utils.SESUtils.Email
import uk.gov.nationalarchives.scannotifications.decoders.IncomingEvent
import uk.gov.nationalarchives.scannotifications.decoders.SSMMaintenanceDecoder.SSMMaintenanceEvent
import uk.gov.nationalarchives.scannotifications.decoders.ScanDecoder.ScanEvent
import uk.gov.nationalarchives.scannotifications.messages.EventMessages._

trait Messages[T <: IncomingEvent] {
  def email(incomingEvent: T): Option[Email]

  def slack(incomingEvent: T): Option[String]
}

object Messages {

  implicit val incomingEventMessages: Messages[IncomingEvent] = new Messages[IncomingEvent] {

    override def email(event: IncomingEvent): Option[Email] = event match {
      case scanEvent: ScanEvent => scanEventMessages.email(scanEvent)
      case ssMMaintenanceEvent: SSMMaintenanceEvent => maintenanceEventMessages.email(ssMMaintenanceEvent)
    }

    override def slack(event: IncomingEvent): Option[String] = event match {
      case scanEvent: ScanEvent => scanEventMessages.slack(scanEvent)
      case ssMMaintenanceEvent: SSMMaintenanceEvent => maintenanceEventMessages.slack(ssMMaintenanceEvent)
    }

  }

  implicit class IncomingEventHelpers(incomingEvent: IncomingEvent)(implicit messages: Messages[IncomingEvent]) {
    def shouldSendEmail: Boolean = messages.email(incomingEvent).isDefined

    def shouldSendSlack: Boolean = messages.slack(incomingEvent).isDefined

    def sendEmailMessage: IO[String] = {
      IO.fromTry(SESUtils(ses).sendEmail(messages.email(incomingEvent).get).map(_.messageId()))
    }

    def sendSlackMessage: IO[String] = {
      implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

      AsyncHttpClientCatsBackend[IO]().flatMap { backend =>
        val request = basicRequest
          .post(uri"${ConfigFactory.load.getString("slack.webhook.url")}")
          .body(messages.slack(incomingEvent).get)
          .contentType(MediaType.ApplicationJson)
        for {
          response <- backend.send(request)
          body <- IO.fromEither(response.body.left.map(e => new RuntimeException(e)))
        } yield body
      }
    }

  }





}
