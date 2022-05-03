package uk.gov.nationalarchives.notifications

import java.util.UUID

import org.scalatest.prop.TableFor6
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportSuccessDetails
import uk.gov.nationalarchives.notifications.decoders.TransformEngineRetryDecoder.TransformEngineRetryEvent

class TransformEngineRetryIntegrationSpec extends LambdaIntegrationSpec {

  override lazy val events: TableFor6[String, String, Option[String], Option[String], Option[SqsExpectedMessageDetails], () => Unit] = Table(
    ("description", "input", "emailBody", "slackBody", "sqsMessage", "stubContext"),
    ("a judgment transform engine retry event on intg",
      transformEngineRetryEventInputText(judgmentRetryEvent), None, None, Some(SqsExpectedMessageDetails(successDetails, 2)), () => ()),
    ("a standard transform engine retry event on intg",
      transformEngineRetryEventInputText(standardRetryEvent), None, None, None, () => ()),
    ("a judgment transform engine event on staging",
      transformEngineRetryEventInputText(judgmentRetryEvent), None, None, Some(SqsExpectedMessageDetails(successDetails, 2)), () => ()),
    ("a standard transform engine retry event on staging",
      transformEngineRetryEventInputText(standardRetryEvent), None, None, None, () => ())
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
