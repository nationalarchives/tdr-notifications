package uk.gov.nationalarchives.notifications

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, postRequestedFor, urlEqualTo}
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor6}
import software.amazon.awssdk.services.sqs.model.Message

trait LambdaIntegrationSpec extends LambdaSpecUtils with TableDrivenPropertyChecks {
  def events: TableFor6[String, String, Option[String], Option[String], Option[String], () => ()]

  forAll(events) {
    (description, input, emailBody, slackBody, sqsMessage, stubContext) => {
      emailBody match {
        case Some(body) =>
          "the process method" should s"send an email message for $description" in {
            stubContext()
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)
            wiremockSesEndpoint.verify(1,
              postRequestedFor(urlEqualTo("/"))
                .withRequestBody(equalTo(body))
            )
          }
        case None =>
          "the process method" should s"not send an email message for $description" in {
            stubContext()
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)
            wiremockSesEndpoint.verify(0, postRequestedFor(urlEqualTo("/")))
          }
      }

      slackBody match {
        case Some(body) =>
          "the process method" should s"send a slack message for $description" in {
            stubContext()
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)
            wiremockSlackServer.verify(slackBody.size,
              postRequestedFor(urlEqualTo("/webhook"))
                .withRequestBody(equalToJson(body))
            )
          }
        case None =>
          "the process method" should s"not send a slack message for $description" in {
            stubContext()
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)
            wiremockSlackServer.verify(0,
              postRequestedFor(urlEqualTo("/webhook"))
            )
          }
      }

      sqsMessage match {
        case Some(expectedBody) =>
          "the process method" should s"send a sqs message for $description" in {
            stubContext()
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)
            val messages: List[Message] = transformEngineQueueHelper.receive

            messages.size shouldBe 1
            val messageBody = messages.head.body()
            messageBody shouldEqual expectedBody
          }
        case None =>
          "the process method" should s"not send a sqs message for $description" in {
            stubContext()
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)
            val messages: List[Message] = transformEngineQueueHelper.receive

            messages.size shouldBe 0
          }
      }
    }
  }
}
