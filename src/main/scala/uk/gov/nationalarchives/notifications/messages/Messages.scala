package uk.gov.nationalarchives.notifications.messages

import cats.effect.{ContextShift, IO}
import cats.implicits._
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import io.circe.syntax._
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.{basicRequest, _}
import sttp.model.MediaType
import uk.gov.nationalarchives.aws.utils.Clients.{kms, ses}
import uk.gov.nationalarchives.aws.utils.{KMSUtils, SESUtils}
import uk.gov.nationalarchives.aws.utils.SESUtils.Email
import uk.gov.nationalarchives.notifications.decoders.IncomingEvent
import uk.gov.nationalarchives.notifications.messages.EventMessages.SlackMessage

trait Messages[T <: IncomingEvent, TContext] {
  def context(incomingEvent: T): IO[TContext]

  def email(incomingEvent: T, context: TContext): Option[Email]

  def slack(incomingEvent: T, context: TContext): Option[SlackMessage]
}

object Messages {
  val config = ConfigFactory.load
  val kmsUtils: KMSUtils = KMSUtils(kms(config.getString("kms.endpoint")), Map("LambdaFunctionName" -> config.getString("function.name")))
  val eventConfig: Map[String, String] = kmsUtils.decryptValuesFromConfig(List("alerts.ecr-scan.mute", "ses.email.to", "slack.webhook.url"))

  def sendMessages[T <: IncomingEvent, TContext](incomingEvent: T)(implicit messages: Messages[T, TContext]): IO[String] = {
    for {
      context <- messages.context(incomingEvent)
      result <- (sendEmailMessage(incomingEvent, context) |+| sendSlackMessage(incomingEvent, context))
        .getOrElse(IO.pure("No messages have been sent"))
    } yield result
  }

  private def sendEmailMessage[T <: IncomingEvent, TContext](incomingEvent: T, context: TContext)(implicit messages: Messages[T, TContext]): Option[IO[String]] = {
    messages.email(incomingEvent, context).map(email => {
      IO.fromTry(SESUtils(ses).sendEmail(email).map(_.messageId()))
    })
  }

  private def sendSlackMessage[T <: IncomingEvent, TContext](incomingEvent: T, context: TContext)(implicit messages: Messages[T, TContext]): Option[IO[String]] = {
    messages.slack(incomingEvent, context).map(slackMessage => {
      implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

      AsyncHttpClientCatsBackend[IO]().flatMap { backend =>
        val request = basicRequest
          .post(uri"${eventConfig("slack.webhook.url")}")
          .body(slackMessage.asJson.noSpaces)
          .contentType(MediaType.ApplicationJson)
        for {
          response <- backend.send(request)
          body <- IO.fromEither(response.body.left.map(e => new RuntimeException(e)))
        } yield body
      }
    })
  }
}
