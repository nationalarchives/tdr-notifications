package uk.gov.nationalarchives.notifications

import java.util.UUID

import org.scalatest.prop.TableFor6
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.{ExportStatusEvent, ExportSuccessDetails}
import uk.gov.nationalarchives.notifications.decoders.TransformEngineRetryDecoder.TransformEngineRetryEvent

class TransformEngineRetryIntegrationSpec extends LambdaIntegrationSpec {

  override lazy val events: TableFor6[String, String, Option[String], Option[String], Option[(ExportSuccessDetails, Int)], () => ()] = Table(
    ("description", "input", "emailBody", "slackBody", "sqsMessage", "stubContext"),
    ("a transform engine retry event on intg",
      transformEngineRetryEventInputText(retryEvent), None, None, Some(successDetails, 2), () => ()),
    ("a transform engine event on staging",
      transformEngineRetryEventInputText(retryEvent), None, None, Some(successDetails, 2), () => ())
  )

  private lazy val successDetails = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "tb-body1", "judgment", "judgment-export-bucket")
  private lazy val retryEvent = TransformEngineRetryEvent("consignmentRef1", 2)

  private def transformEngineRetryEventInputText(retryEvent: TransformEngineRetryEvent): String = {
    val consignmentRef = retryEvent.consignmentReference
    val retryCount = retryEvent.retryCount

    s"""
       |{
       |  "Records": [
       |        {
       |            "messageId": "messageIdValue",
       |            "receiptHandle": "receipt handle value",
       |            "body": "{\\"consignmentReference\\": \\"$consignmentRef\\",\\"retryCount\\": $retryCount}",
       |            "attributes": {
       |                "ApproximateReceiveCount": "1",
       |                "SentTimestamp": "1545082649183",
       |                "SenderId": "senderIdValue",
       |                "ApproximateFirstReceiveTimestamp": "1545082649185"
       |            },
       |            "messageAttributes": {},
       |            "md5OfBody": "md5OfBodyValue",
       |            "eventSource": "aws:sqs",
       |            "eventSourceARN": "queueArn",
       |            "awsRegion": "eu-west-2"
       |        }
       |  ]
       |}
       |""".stripMargin
  }

  private def expectedSqsMessage(exportStatusEvent: ExportStatusEvent): Option[ExportSuccessDetails] = {
      Some(exportStatusEvent.successDetails.get)
  }
}
