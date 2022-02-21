package uk.gov.nationalarchives.notifications

import org.scalatest.prop.TableFor6
import uk.gov.nationalarchives.notifications.decoders.TransformEngineRetryDecoder.TransformEngineRetryEvent

class TransformEngineRetrySpec extends LambdaIntegrationSpec {

  override lazy val events: TableFor6[String, String, Option[String], Option[String], Option[String], () => ()] = Table(
    ("description", "input", "emailBody", "slackBody", "sqsMessage", "stubContext"),
    ("a transform engine retry event on intg",
      exportStatusEventInputText(transformEngineRetry), None, None, expectedSqsMessage(transformEngineRetry), () => ()),
    ("a transform engine retry event on staging",
      exportStatusEventInputText(transformEngineRetry), None, None, expectedSqsMessage(transformEngineRetry), () => ())
  )

  private lazy val transformEngineRetry = TransformEngineRetryEvent("consignmentReference", 1)

  private def exportStatusEventInputText(transformEngineRetryEvent: TransformEngineRetryEvent): String = {
    val consignmentReference: String = transformEngineRetryEvent.consignmentReference
    val retryCount: Int = transformEngineRetryEvent.retryCount
    s"""
       |{
       |  "Records": [
       |        {
       |            "messageId": "messageIdValue",
       |            "receiptHandle": "receipt handle value",
       |            "body": "{\\"consignmentReference\\": \\"$consignmentReference\\",\\"retryCount\\": $retryCount}",
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

  private def expectedSqsMessage(retryEvent: TransformEngineRetryEvent): Option[String] = {
      val consignmentReference = retryEvent.consignmentReference
      val retryCount = retryEvent.retryCount
      Some(s"""{
         |  "packageSignedUrl" : "placeholder_value",
         |  "packageShaSignedUrl" : "placeholder_value",
         |  "consignmentReference" : "$consignmentReference",
         |  "retryCount" : $retryCount
         |}""".stripMargin)
  }
}
