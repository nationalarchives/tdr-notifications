package uk.gov.nationalarchives.notifications

import org.scalatest.prop.TableFor8
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportSuccessDetails
import uk.gov.nationalarchives.notifications.decoders.TransformEngineRetryDecoder.TransformEngineRetryEvent

import java.util.UUID

class TransformEngineRetryIntegrationSpec extends LambdaIntegrationSpec {

  override lazy val events: TableFor8[String, String, Option[String], Option[String], Option[SqsExpectedMessageDetails], Option[SnsExpectedMessageDetails], () => Unit, String] = Table(
    ("description", "input", "emailBody", "slackBody", "sqsMessage", "snsMessage", "stubContext", "slackUrl"),
    ("a judgment transform engine retry event on intg",
      transformEngineRetryEventInputText(judgmentRetryEvent), None, None, Some(SqsExpectedMessageDetails(successDetails, 2)), None, () => (), "/webhook"),
    ("a standard transform engine retry event on intg",
      transformEngineRetryEventInputText(standardRetryEvent), None, None, None, None, () => (), "/webhook"),
    ("a judgment transform engine event on staging",
      transformEngineRetryEventInputText(judgmentRetryEvent), None, None, Some(SqsExpectedMessageDetails(successDetails, 2)), None, () => (), "/webhook"),
    ("a standard transform engine retry event on staging",
      transformEngineRetryEventInputText(standardRetryEvent), None, None, None, None, () => (), "/webhook")
  )

  private lazy val successDetails = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "tb-body1", "judgment", "judgment-export-bucket")
  private lazy val judgmentRetryEvent = TransformEngineRetryEvent("consignmentRef1", "judgment", 2)
  private lazy val standardRetryEvent = TransformEngineRetryEvent("consignmentRef1", "standard", 2)

  private def transformEngineRetryEventInputText(retryEvent: TransformEngineRetryEvent): String = {
    val consignmentRef = retryEvent.consignmentReference
    val consignmentType = retryEvent.consignmentType
    val retryCount = retryEvent.numberOfRetries

    s"""
       |{
       |  "Records": [
       |        {
       |            "body": "{\\"consignment-reference\\": \\"$consignmentRef\\",\\"consignment-type\\": \\"$consignmentType\\", \\"number-of-retries\\": $retryCount}"
       |        }
       |  ]
       |}
       |""".stripMargin
  }
}
