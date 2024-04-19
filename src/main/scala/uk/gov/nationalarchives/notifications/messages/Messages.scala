package uk.gov.nationalarchives.notifications.messages

import cats.effect.IO
import cats.implicits._
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.generic.auto._
import io.circe.syntax._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.{basicRequest, _}
import sttp.model.MediaType
import uk.gov.nationalarchives.aws.utils.kms.KMSClients.kms
import uk.gov.nationalarchives.aws.utils.kms._
import uk.gov.nationalarchives.aws.utils.ses._
import uk.gov.nationalarchives.aws.utils.sns._
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportStatusEvent
import uk.gov.nationalarchives.notifications.decoders.IncomingEvent
import uk.gov.nationalarchives.notifications.decoders.KeycloakEventDecoder.KeycloakEvent
import uk.gov.nationalarchives.notifications.messages.EventMessages.{SlackMessage, SnsMessageDetails, SqsMessageDetails}

trait Messages[T <: IncomingEvent, TContext] {
  def context(incomingEvent: T): IO[TContext]

  def email(incomingEvent: T, context: TContext): Option[SESUtils.Email]

  def slack(incomingEvent: T, context: TContext): Option[SlackMessage]

  def sns(incomingEvent: T, context: TContext): Option[SnsMessageDetails] = None
}

object Messages {
  val config: Config = ConfigFactory.load
  val kmsUtils: KMSUtils = KMSUtils(kms(config.getString("kms.endpoint")), Map("LambdaFunctionName" -> config.getString("function.name")))
  val eventConfig: Map[String, String] = List(
    "alerts.ecr-scan.mute",
    "ses.email.to",
    "slack.webhook.url",
    "slack.webhook.judgment_url",
    "slack.webhook.standard_url",
    "slack.webhook.tdr_url",
    "slack.webhook.export_url",
    "sns.topic.da_event_bus_arn"
  ).map(configName => configName -> kmsUtils.decryptValue(config.getString(configName))).toMap

  def sendMessages[T <: IncomingEvent, TContext](incomingEvent: T)(implicit messages: Messages[T, TContext]): IO[String] = {
    for {
      context <- messages.context(incomingEvent)
      result <- (sendEmailMessage(incomingEvent, context) |+| sendSlackMessage(incomingEvent, context)
        |+| sendSNSMessage(incomingEvent, context))
        .getOrElse(IO.pure("No messages have been sent"))
    } yield result
  }

  private def sendEmailMessage[T <: IncomingEvent, TContext](incomingEvent: T, context: TContext)(implicit messages: Messages[T, TContext]): Option[IO[String]] = {
    messages.email(incomingEvent, context).map(email => {
      IO.fromTry(SESUtils(SESClients.ses(config.getString("ses.endpoint"))).sendEmail(email).map(_.messageId()))
    })
  }

  private def sendSNSMessage[T <: IncomingEvent, TContext](incomingEvent: T, context: TContext)(implicit messages: Messages[T, TContext]): Option[IO[String]] = {
    messages.sns(incomingEvent, context).map(snsMessageDetails => {
      val endpoint = config.getString("sns.endpoint")
      val messageBody = snsMessageDetails.messageBody
      val topicArn = snsMessageDetails.snsTopic
      IO(SNSUtils(SNSClients.sns(endpoint)).publish(messageBody, topicArn).toString)
    })
  }

  private def sendSlackMessage[T <: IncomingEvent, TContext](incomingEvent: T, context: TContext)(implicit messages: Messages[T, TContext]): Option[IO[String]] = {
    messages.slack(incomingEvent, context).map(slackMessage => {
      val url = incomingEvent match {
        case ev: ExportStatusEvent if ev.environment == "prod" && ev.successDetails.exists(_.consignmentType == "judgment") =>
          eventConfig("slack.webhook.judgment_url")
        case ev: ExportStatusEvent if ev.environment == "prod" && ev.successDetails.exists(_.consignmentType == "standard") =>
          eventConfig("slack.webhook.standard_url")
        case _: ExportStatusEvent => eventConfig("slack.webhook.export_url")
        case _: KeycloakEvent => eventConfig("slack.webhook.tdr_url")
        case _ => eventConfig("slack.webhook.url")
      }
      val requestBody = slackMessage.asJson.noSpaces
      AsyncHttpClientCatsBackend.resource[IO]().use { backend =>
        val request = basicRequest
          .post(uri"$url")
          .body(requestBody)
          .contentType(MediaType.ApplicationJson)
        for {
          response <- backend.send(request)
          body <- IO.fromEither(response.body.left.map(e => new RuntimeException(e)))
        } yield body
      }
    })
  }
}
