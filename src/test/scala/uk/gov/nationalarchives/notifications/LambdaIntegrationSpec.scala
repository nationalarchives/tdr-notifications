package uk.gov.nationalarchives.notifications

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, postRequestedFor, urlEqualTo}
import io.circe.generic.auto._
import io.circe.parser
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor6}
import software.amazon.awssdk.services.sqs.model.Message
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportSuccessDetails

trait LambdaIntegrationSpec extends LambdaSpecUtils with TableDrivenPropertyChecks {
  def events: TableFor6[String, String, Option[String], Option[String], Option[ExportSuccessDetails], () => ()]

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
        case Some(expectedSuccessDetails) =>
          "the process method" should s"send a sqs message for $description" in {
            stubContext()
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)
            val messages: List[Message] = transformEngineQueueHelper.receive

            messages.size shouldBe 1
            val messageBody = messages.head.body()
            val message = parser.decode[SqsExportMessageBody](messageBody) match {
              case Right(value) => value
            }

            val expectedConsignmentRef = expectedSuccessDetails.consignmentReference
            val expectedBucket = expectedSuccessDetails.exportBucket
            val expectedSignedUrl = s"https://s3.eu-west-2.amazonaws.com/${expectedBucket}/${expectedConsignmentRef}.tar.gz?X-Amz-Security-Token"
            val expectedShaSignedUrl = s"https://s3.eu-west-2.amazonaws.com/${expectedBucket}/${expectedConsignmentRef}.tar.gz.sha256?X-Amz-Security-Token"

            message.consignmentReference shouldEqual expectedConsignmentRef
            message.retryCount shouldBe 0
            message.packageSignedUrl.startsWith(expectedSignedUrl) shouldBe true
            message.packageShaSignedUrl.startsWith(expectedShaSignedUrl) shouldBe true
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

case class SqsExportMessageBody(packageSignedUrl: String,
                                packageShaSignedUrl: String,
                                consignmentReference: String,
                                retryCount: Int)
