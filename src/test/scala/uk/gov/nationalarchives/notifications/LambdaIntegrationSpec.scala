package uk.gov.nationalarchives.notifications

import com.github.tomakehurst.wiremock.client.WireMock._
import io.circe.Json
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import uk.gov.nationalarchives.da.messages.bag.available.BagAvailable
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportSuccessDetails
import uk.gov.nationalarchives.notifications.messages.EventMessages.GovUKEmailDetails

import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8
import scala.jdk.CollectionConverters._

trait LambdaIntegrationSpec extends LambdaSpecUtils {
  def events: Seq[Event]

  private def snsMessageJson(formBody: String): Json = {
    val formParams = formBody.split("&").map { pair =>
      pair.split("=", 2) match {
        case Array(key, value) => URLDecoder.decode(key, UTF_8) -> URLDecoder.decode(value, UTF_8)
        case Array(key) => URLDecoder.decode(key, UTF_8) -> ""
      }
    }.toMap
    parse(formParams("Message")).fold(throw _, identity)
  }

  events.foreach { event => {
    import event._
    expectedOutput.emailBody match {
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
      
      expectedOutput.slackMessage match {
        case Some(message) =>
          "the process method" should s"send a slack message for $description" in {
            stubContext()
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)
            wiremockSlackServer.verify(1,
              postRequestedFor(urlEqualTo(message.webhookUrl))
                .withRequestBody(equalToJson(message.body))
            )
          }
        case None =>
          "the process method" should s"not send a slack message for $description" in {
            stubContext()
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)
            wiremockSlackServer.verify(0,
              postRequestedFor(anyUrl())
            )
          }
      }

      expectedOutput.snsMessage match {
        case Some(expectedDetails) =>
          "the process method" should s"send an sns message for $description" in {
            stubContext()
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)

            val requests = wiremockSnsEndpoint.findAll(postRequestedFor(urlEqualTo("/"))).asScala
            requests.size should equal(1)

            val message = snsMessageJson(requests.head.getBodyAsString)
            val properties = message.hcursor.downField("properties")
            val parameters = message.hcursor.downField("parameters")
            val reference = expectedDetails.consignmentReference

            properties.get[String]("messageType") shouldBe Right(classOf[BagAvailable].getCanonicalName)
            properties.get[String]("function") shouldBe Right("tdr-export-process")
            properties.get[String]("producer") shouldBe Right("TDR")
            properties.get[String]("timestamp").isRight shouldBe true
            properties.get[String]("executionId").isRight shouldBe true
            properties.downField("parentExecutionId").succeeded shouldBe true

            parameters.get[String]("reference") shouldBe Right(reference)
            parameters.get[String]("originator") shouldBe Right("TDR")
            parameters.get[Option[List[String]]]("series") shouldBe Right(expectedDetails.series.map(List(_)))
            parameters.get[List[String]]("transferringBodies") shouldBe Right(List(expectedDetails.transferringBodyNames))
            parameters.get[String]("consignmentType").isRight shouldBe true
            parameters.get[String]("s3Bucket") shouldBe Right(expectedDetails.bucketName)
            parameters.get[String]("s3BagKey") shouldBe Right(s"$reference.tar.gz")
            parameters.get[String]("s3BagSha256Key") shouldBe Right(s"$reference.tar.gz.sha256")
          }
        case None =>
          "the process method" should s"not send a sns message for $description" in {
            stubContext()
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)
            wiremockSnsEndpoint.verify(0, postRequestedFor(urlEqualTo("/")))
          }
      }
      
    expectedOutput.govUKEmail match {
      case Some(email) =>
        "the process method" should s"send a gov uk notify email request for $description" in {
          stubContext()
          val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
          new Lambda().process(stream, null)
          wiremockGovUkNotifyEndpoint.verify(1,
            postRequestedFor(urlEqualTo("/v2/notifications/email"))
              .withRequestBody(
                equalToJson(
                  s"""
                    |{
                    |  "reference": "${email.reference}",
                    |  "email_address": "${email.userEmail}",
                    |  "personalisation": ${email.personalisation.asJson},
                    |  "template_id": "${email.templateId}"
                    |}
                    |""".stripMargin
                )
              )
          )
        }
      case None =>
        "the process method" should s"not send a gov uk notify email for $description" in {
          stubContext()
          val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
          new Lambda().process(stream, null)
          wiremockGovUkNotifyEndpoint.verify(0,
            postRequestedFor(anyUrl())
          )
        }
      }
    }
  }
}

case class Event(
  description: String,
  input: String,
  stubContext: () => Unit = () => (),
  expectedOutput: ExpectedOutput,
)

case class ExpectedOutput(
  emailBody: Option[String] = None,
  slackMessage: Option[SlackMessage] = None,
  sqsMessage: Option[SqsExpectedMessageDetails] = None,
  snsMessage: Option[SnsExpectedMessageDetails] = None,
  govUKEmail: Option[GovUKEmailDetails] = None
)

case class SlackMessage(body: String, webhookUrl: String)
case class SqsExpectedMessageDetails(successDetails: ExportSuccessDetails, retryCount: Int)

case class SnsExpectedMessageDetails(consignmentReference: String,
                                     consignmentType: String,
                                     series: Option[String],
                                     transferringBodyNames: String,
                                     bucketName: String,
                                     environment: String)
