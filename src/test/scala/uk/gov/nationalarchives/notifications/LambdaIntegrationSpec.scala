package uk.gov.nationalarchives.notifications

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor8}
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportSuccessDetails

trait LambdaIntegrationSpec extends LambdaSpecUtils with TableDrivenPropertyChecks {
  def events: TableFor8[String, String, Option[String], Option[String], Option[SqsExpectedMessageDetails], Option[SnsExpectedMessageDetails], () => Unit, String]

  forAll(events) {
    (description, input, emailBody, slackBody, sqsMessage, snsMessage, stubContext, slackUrl) => {
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
              postRequestedFor(urlEqualTo(slackUrl))
                .withRequestBody(equalToJson(body))
            )
          }
        case None =>
          "the process method" should s"not send a slack message for $description" in {
            stubContext()
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)
            wiremockSlackServer.verify(0,
              postRequestedFor(urlEqualTo(slackUrl))
            )
          }
      }

      snsMessage match {
        case Some(expectedDetails) =>
          "the process method" should s"send an sns message for $description" in {
            stubContext()
            val fieldValueSeparator: String = "%22%3A%22"
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)
            wiremockSnsEndpoint.verify(1,
              postRequestedFor(urlEqualTo("/"))
                .withRequestBody(containing("properties"))
                .withRequestBody(containing(s"messageType${fieldValueSeparator}uk.gov.nationalarchives.da.messages.bag.available.BagAvailable"))
                .withRequestBody(containing("timestamp"))
                .withRequestBody(containing("function" + fieldValueSeparator + "tdr-export-process"))
                .withRequestBody(containing(s"producer${fieldValueSeparator}TDR"))
                .withRequestBody(containing("executionId"))
                .withRequestBody(containing("parentExecutionId"))
                .withRequestBody(containing("parameters"))
                .withRequestBody(containing(s"reference${fieldValueSeparator}${expectedDetails.consignmentReference}"))
                .withRequestBody(containing(s"originator${fieldValueSeparator}TDR"))
                .withRequestBody(containing(s"consignmentType"))
                .withRequestBody(containing(s"Bucket${fieldValueSeparator}${expectedDetails.bucketName}"))
                .withRequestBody(containing(s"s3BagKey${fieldValueSeparator}${expectedDetails.consignmentReference}.tar.gz"))
                .withRequestBody(containing(s"s3BagSha256Key${fieldValueSeparator}${expectedDetails.consignmentReference}.tar.gz.sha256"))
            )
          }
        case None =>
          "the process method" should s"not send a sns message for $description" in {
            stubContext()
            val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
            new Lambda().process(stream, null)
            wiremockSnsEndpoint.verify(0, postRequestedFor(urlEqualTo("/")))
          }
      }
    }
  }
}

case class SqsExpectedMessageDetails(successDetails: ExportSuccessDetails, retryCount: Int)

case class SnsExpectedMessageDetails(consignmentReference: String,
                                     consignmentType: String,
                                     bucketName: String,
                                     environment: String)
