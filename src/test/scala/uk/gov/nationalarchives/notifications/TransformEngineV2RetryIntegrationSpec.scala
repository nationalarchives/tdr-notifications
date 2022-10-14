package uk.gov.nationalarchives.notifications

import org.scalatest.prop.TableFor8
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportSuccessDetails
import uk.gov.nationalarchives.notifications.decoders.TransformEngineRetryDecoder.TransformEngineRetryEvent
import uk.gov.nationalarchives.notifications.decoders.TransformEngineV2Decoder.{BagitValidationError, ErrorParameters, Producer, TransformEngineV2RetryEvent}

import java.util.UUID

class TransformEngineV2RetryIntegrationSpec extends LambdaIntegrationSpec {

  override lazy val events: TableFor8[String, String, Option[String], Option[String], Option[SqsExpectedMessageDetails], Option[String], () => Unit, String] = Table(
    ("description", "input", "emailBody", "slackBody", "sqsMessage", "snsMessage", "stubContext", "slackUrl"),
    ("a judgment transform engine retry event on intg",
      transformEngineRetryEventInputText(judgmentRetryEvent), None, None, None, expectedOutputText(judgmentRetryEvent), () => (), "/webhook"),
//    ("a standard transform engine retry event on intg",
//      transformEngineRetryEventInputText(standardRetryEvent), None, None, None, expectedOutputText(standardRetryEvent), () => (), "/webhook"),
//    ("a judgment transform engine event on staging",
//      transformEngineRetryEventInputText(judgmentRetryEvent), None, None, None, expectedOutputText(judgmentRetryEvent), () => (), "/webhook"),
//    ("a standard transform engine retry event on staging",
//      transformEngineRetryEventInputText(standardRetryEvent), None, None, None, expectedOutputText(standardRetryEvent), () => (), "/webhook"),
//    ("a judgment transform engine event on prod",
//      transformEngineRetryEventInputText(judgmentRetryEvent), None, None, None, expectedOutputText(judgmentRetryEvent), () => (), "/webhook"),
//    ("a standard transform engine retry event on prod",
//      transformEngineRetryEventInputText(standardRetryEvent), None, None, None, expectedOutputText(standardRetryEvent), () => (), "/webhook")
  )

  private lazy val judgmentRetryEvent = createRetryEvent("judgment")
  private lazy val standardRetryEvent = createRetryEvent("standard")

  private def transformEngineRetryEventInputText(retryEvent: TransformEngineV2RetryEvent): String = {
    val consignmentType = retryEvent.producer.`type`

    s"""
       |{
       |  "Records": [
       |        {
       |          "body": "{\\"version\\":\\"1.0.0\\",\\"timestamp\\":1661340417609575000,\\"UUIDs\\": [{\\"TDR-UUID\\": \\"c73e5ca7-cf87-442a-8248-e05f81361ae0\\"},{\\"TRE-UUID\\": \\"ec506d7f-f531-4e63-833e-841918105e41\\"}],\\"producer\\": {\\"environment\\": \\"dev\\",\\"name\\": \\"TRE\\",\\"process\\": \\"dev-tre-validate-bagit\\",\\"event-name\\": \\"bagit-validation-error\\",\\"type\\": \\"$consignmentType\\"},\\"parameters\\": {\\"bagit-validation-error\\": {\\"reference\\": \\"ABC-1234-DEF\\",\\"errors\\": [\\"some error message\\"]}}}"
       |        }
       |    ]
       |}
       |""".stripMargin
  }

  private def expectedOutputText(retryEvent: TransformEngineV2RetryEvent): Option[String] = {
    val consignmentType: String = retryEvent.producer.`type`
    val consignmentRef: String = retryEvent.parameters.`bagit-validation-error`.reference
    val bucket: String = if (consignmentType == "judgment") {
      "judgment-export-bucket"
    } else {
      "standard-export-bucket"
    }

    val outputText =
      s"""
         | {
         |  "Records": {
         |    "Sns": {
         |      "Message": "{
         |        \\"version\\": \\"1.0.0\\",
         |        \\"timestamp\\": 1661155064747274000,
         |        \\"UUIDs\\": [
         |          {
         |            \\"TDR-UUID\\": \\"c73e5ca7-cf87-442a-8248-e05f81361ae0\\"
         |          },
         |          {
         |            \\"TRE-UUID\\": \\"ec506d7f-f531-4e63-833e-841918105e41\\"
         |          },
         |          {
         |            \\"TRE-UUID\\": \\"3c1db304-090f-4b19-abfc-8618cc0e5875\\"
         |          }
         |        ],
         |        \\"producer\\": {
         |          \\"environment\\": \\"dev\\",
         |          \\"name\\": \\"TDR\\",
         |          \\"process\\": \\"tdr-export-process\\",
         |          \\"event-name\\": \\"new-bagit\\",
         |          \\"type\\": \\"$consignmentType\\"
         |         },
         |         \\"parameters\\": {
         |           \\"new-bagit\\": {
         |             \\"resource\\": {
         |               \\"resource-type\\": \\"Object\\",
         |               \\"access-type\\": \\"url\\",
         |               \\"value\\": \\"https://s3.eu-west-2.amazonaws.com/$bucket/$consignmentRef.tar.gz?X-Amz-...\\"
         |              },
         |            \\"resource-validation\\": {
         |              \\"resource-type\\": \\"Object\\",
         |              \\"access-type\\": \\"url\\",
         |              \\"validation-method\\": \\"SHA256\\",
         |              \\"value\\": \\"https://s3.eu-west-2.amazonaws.com/$bucket/$consignmentRef.tar.gz.sha256?X-Amz-...\\"
         |             },
         |             \\"reference\\": \\"$consignmentRef\\"
         |           }
         |         }
         |      }"
         |    }
         |  }
         |}
         |""".stripMargin

    Some(outputText)
  }

  private def createRetryEvent(consignmentType: String): TransformEngineV2RetryEvent = {
    val producer = Producer("dev", "tre", "dev-tre-validate-bagit", "bagit-validation-error", consignmentType)
    val validationError = BagitValidationError("ABC-1234-DEF", Some(List("some error message")))
    val parameters = ErrorParameters(validationError)
    TransformEngineV2RetryEvent("1.0.0", 1661340417609575000L, List(Map()), producer, parameters)
  }
}
