package uk.gov.nationalarchives.scannotifications.messages

import cats.effect.{ContextShift, IO}
import com.typesafe.config.ConfigFactory
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.{basicRequest, _}
import sttp.model.MediaType
import cats.implicits._
import uk.gov.nationalarchives.aws.utils.Clients.ses
import uk.gov.nationalarchives.aws.utils.SESUtils
import uk.gov.nationalarchives.aws.utils.SESUtils.Email
import uk.gov.nationalarchives.scannotifications.decoders.IncomingEvent

trait Messages[T <: IncomingEvent] {
  def email(incomingEvent: T): Option[Email]

  def slack(incomingEvent: T): Option[String]
}

object Messages {

  def sendMessages[T <: IncomingEvent](incomingEvent: T)(implicit messages: Messages[T]): IO[String] = {
    IO.fromOption (
      sendEmailMessage(incomingEvent) |+| sendSlackMessage(incomingEvent)
    )(new RuntimeException(s"No recipients configured for event $incomingEvent")).flatten
  }

  def sendEmailMessage[T <: IncomingEvent](incomingEvent: T)(implicit messages: Messages[T]): Option[IO[String]] = {
    messages.email(incomingEvent).map(email => {
      IO.fromTry(SESUtils(ses).sendEmail(email).map(_.messageId()))
    })
  }

  def sendSlackMessage[T <: IncomingEvent](incomingEvent: T)(implicit messages: Messages[T]): Option[IO[String]] = {
    messages.slack(incomingEvent).map(slackMessage => {
      implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

      AsyncHttpClientCatsBackend[IO]().flatMap { backend =>
        val request = basicRequest
          .post(uri"${ConfigFactory.load.getString("slack.webhook.url")}")
          .body(slackMessage)
          .contentType(MediaType.ApplicationJson)
        for {
          response <- backend.send(request)
          body <- IO.fromEither(response.body.left.map(e => new RuntimeException(e)))
        } yield body
      }
    })
  }
}
