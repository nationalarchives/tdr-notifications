package uk.gov.nationalarchives.notifications

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.nationalarchives.aws.utils.ses.SESUtils
import uk.gov.nationalarchives.notifications.decoders.GenericMessageDecoder.GenericMessagesEvent
import uk.gov.nationalarchives.notifications.messages.EventMessages.{GovUKEmailDetails, SlackMessage, SnsMessageDetails}
import uk.gov.nationalarchives.notifications.messages.Messages

class MessagesSpec extends LambdaSpecUtils {

  private val event = GenericMessagesEvent(Nil)

  private class TestMessages extends Messages[GenericMessagesEvent, Unit] {
    var emailPrepared = false

    override def context(incomingEvent: GenericMessagesEvent): IO[Unit] = IO.unit

    override def email(incomingEvent: GenericMessagesEvent, context: Unit): Option[SESUtils.Email] = {
      emailPrepared = true
      None
    }

    override def slack(incomingEvent: GenericMessagesEvent, context: Unit): Option[SlackMessage] =
      Some(SlackMessage(Nil))

    override def sns(incomingEvent: GenericMessagesEvent, context: Unit): Option[SnsMessageDetails] =
      Some(SnsMessageDetails("test-topic", "test-message"))

    override def govUkNotifyEmail(incomingEvent: GenericMessagesEvent, context: Unit): List[GovUKEmailDetails] =
      List(GovUKEmailDetails("test-template", "test@example.com", Map.empty, "test-reference"))
  }

  "Message delivery" should "skip email preparation in dev while still sending Slack and SNS messages" in {
    implicit val messages: Messages[GenericMessagesEvent, Unit] = new TestMessages {
      override def email(incomingEvent: GenericMessagesEvent, context: Unit): Option[SESUtils.Email] =
        fail("SES email must not be prepared in dev")

      override def govUkNotifyEmail(incomingEvent: GenericMessagesEvent, context: Unit): List[GovUKEmailDetails] =
        fail("Notify email must not be prepared in dev, even if its template is missing")
    }

    Messages.sendMessages(event, "dev").unsafeRunSync()

    wiremockSesEndpoint.verify(0, postRequestedFor(anyUrl()))
    wiremockGovUkNotifyEndpoint.verify(0, postRequestedFor(anyUrl()))
    wiremockSlackServer.verify(1, postRequestedFor(urlEqualTo("/webhook-url")))
    wiremockSnsEndpoint.verify(1, postRequestedFor(urlEqualTo("/")))
  }

  Seq("intg", "staging", "prod").foreach { environment =>
    it should s"preserve email delivery in $environment" in {
      implicit val messages: TestMessages = new TestMessages
      stubDummyGovUkNotifyEmailResponse()

      Messages.sendMessages(event, environment).unsafeRunSync()

      messages.emailPrepared should be(true)
      wiremockGovUkNotifyEndpoint.verify(1, postRequestedFor(urlEqualTo("/v2/notifications/email")))
      wiremockSlackServer.verify(1, postRequestedFor(urlEqualTo("/webhook-url")))
      wiremockSnsEndpoint.verify(1, postRequestedFor(urlEqualTo("/")))
    }
  }
}
