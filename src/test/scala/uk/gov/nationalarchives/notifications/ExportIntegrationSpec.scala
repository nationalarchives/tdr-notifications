package uk.gov.nationalarchives.notifications

import org.scalatest.prop.TableFor8
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.{ExportStatusEvent, ExportSuccessDetails}

import java.util.UUID

class ExportIntegrationSpec extends LambdaIntegrationSpec {

  override lazy val events: TableFor8[String, String, Option[String], Option[String], Option[SqsExpectedMessageDetails], Option[String], () => Unit, String] = Table(
    ("description", "input", "emailBody", "slackBody", "sqsMessage", "snsMessage", "stubContext", "slackUrl"),
    //should send an SNS message
    ("a successful standard export event on intg",
      exportStatusEventInputText(exportStatus1), None, None, None, expectedSnsMessage(exportStatus1), () => (), "/webhook-export"),
    //should not send an SNS message
    ("a successful standard export event using a mock transferring body on intg",
      exportStatusEventInputText(exportStatus3), None, None, None, None, () => (), "/webhook-export"),
    //should send an SNS message
    ("a successful judgment export event on intg",
      exportStatusEventInputText(exportStatus2), None, None, expectedSqsMessage(exportStatus2), None, () => (), "/webhook-export"),
    //should not send an SNS message
    ("a successful judgment export event using a mock transferring body on intg",
      exportStatusEventInputText(exportStatus3), None, None, None, None, () => (), "/webhook-export"),
    //should not send an SNS message
    ("a failed export event on intg",
      exportStatusEventInputText(exportStatus4), None, Some(expectedSlackMessage(exportStatus4)), None, None, () => (), "/webhook-export"),
    //should send an SNS message
    ("a successful standard export event on staging",
      exportStatusEventInputText(exportStatus5), None, Some(expectedSlackMessage(exportStatus5)), None, None, () => (), "/webhook-export"),
    //should not send an SNS message
    ("a successful standard export event using a mock transferring body on staging",
      exportStatusEventInputText(exportStatus7), None, Some(expectedSlackMessage(exportStatus7)), None, None, () => (), "/webhook-export"),
    //should send an SNS message
    ("a successful judgment export event on staging",
      exportStatusEventInputText(exportStatus6), None, Some(expectedSlackMessage(exportStatus6)), expectedSqsMessage(exportStatus6), None, () => (), "/webhook-export"),
    //should not send an SNS message
    ("a successful judgment export event using a mock transferring body on staging",
      exportStatusEventInputText(exportStatus7), None, Some(expectedSlackMessage(exportStatus7)), None, None, () => (), "/webhook-export"),
    //should not send an SNS message
    ("a failed export event on staging",
      exportStatusEventInputText(exportStatus8), None, Some(expectedSlackMessage(exportStatus8)), None, None, () => (), "/webhook-export"),
    //should not send an SNS message
    ("a failed export on intg with no error details",
      exportStatusEventInputText(exportStatus9), None, Some(expectedSlackMessage(exportStatus9)), None, None, () => (), "/webhook-export"),
    //should not send an SNS message
    ("a failed export on staging with no error details",
      exportStatusEventInputText(exportStatus10), None, Some(expectedSlackMessage(exportStatus10)), None, None, () => (), "/webhook-export"),
    //should send an SNS message
    ("a successful standard export event on prod",
      exportStatusEventInputText(exportStatus11), None, Some(expectedSlackMessage(exportStatus11)), None, None, () => (), "/webhook-export"),
    //should not send an SNS message
    ("a failed standard export event on prod",
      exportStatusEventInputText(exportStatus12), None, Some(expectedSlackMessage(exportStatus12)), None, None, () => (), "/webhook-export"),
    //should not send an SNS message
    ("a successful standard export event using a mock transferring body on prod",
      exportStatusEventInputText(exportStatus7), None, Some(expectedSlackMessage(exportStatus7)), None, None, () => (), "/webhook-export"),
    //should send an SNS message
    ("a successful judgment export on prod",
    exportStatusEventInputText(exportStatus13), None, Some(expectedSlackMessage(exportStatus13)), expectedSqsMessage(exportStatus13), None, () => (), "/webhook-judgment")
  )

  private lazy val successDetailsStandard = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "tb-body1", "standard", "export-bucket")
  private lazy val successDetailsJudgment = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "tb-body1", "judgment", "export-bucket")
  private lazy val successDetailsJudgmentMockBody = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "MOCK1 Department", "judgment", "export-bucket")
  private lazy val causeOfFailure = "Cause of failure"
  private lazy val exportStatus1 = ExportStatusEvent(UUID.randomUUID(), success = true, environment = "intg", successDetails = Some(successDetailsStandard), failureCause = None)
  private lazy val exportStatus2 = ExportStatusEvent(UUID.randomUUID(), success = true, "intg", Some(successDetailsJudgment), None)
  private lazy val exportStatus3 = ExportStatusEvent(UUID.randomUUID(), success = true, "intg", Some(successDetailsJudgmentMockBody), None)
  private lazy val exportStatus4 = ExportStatusEvent(UUID.randomUUID(), success = false, "intg", None, Some(causeOfFailure))
  private lazy val exportStatus5 = ExportStatusEvent(UUID.randomUUID(), success = true, "staging", Some(successDetailsStandard), None)
  private lazy val exportStatus6 = ExportStatusEvent(UUID.randomUUID(), success = true, "staging", Some(successDetailsJudgment), None)
  private lazy val exportStatus7 = ExportStatusEvent(UUID.randomUUID(), success = true, "staging", Some(successDetailsJudgmentMockBody), None)
  private lazy val exportStatus8 = ExportStatusEvent(UUID.randomUUID(), success = false, "staging", None, Some(causeOfFailure))
  private lazy val exportStatus9 = ExportStatusEvent(UUID.randomUUID(), success = false, "intg", None, None)
  private lazy val exportStatus10 = ExportStatusEvent(UUID.randomUUID(), success = false, "staging", None, None)
  private lazy val exportStatus11 = ExportStatusEvent(UUID.randomUUID(), success = true, "prod", Some(successDetailsStandard), None)
  private lazy val exportStatus12 = ExportStatusEvent(UUID.randomUUID(), success = false, "prod", None, Some(causeOfFailure))
  private lazy val exportStatus13 = ExportStatusEvent(UUID.randomUUID(), success = true, "prod", Some(successDetailsJudgment), None)

  private def exportStatusEventInputText(exportStatusEvent: ExportStatusEvent): String = {
    val successDetails = exportStatusEvent.successDetails
    val failureCause = exportStatusEvent.failureCause
    val exportOutputJson = if(successDetails.isDefined) {
      val sd = successDetails.get
      s""", \\"successDetails\\":{\\"userId\\": \\"${sd.userId}\\",\\"consignmentReference\\": \\"${sd.consignmentReference}\\",\\"transferringBodyName\\": \\"${sd.transferringBodyName}\\", \\"consignmentType\\": \\"${sd.consignmentType}\\", \\"exportBucket\\": \\"${sd.exportBucket}\\"}"""
    } else if(failureCause.isDefined) s""", \\"failureCause\\":\\"${failureCause.get}\\" """ else """"""

    s"""
       |{
       |    "Records": [
       |        {
       |            "Sns": {
       |                "Message": "{\\"success\\":${exportStatusEvent.success},\\"consignmentId\\":\\"${exportStatusEvent.consignmentId}\\", \\"environment\\": \\"${exportStatusEvent.environment}\\"$exportOutputJson}"
       |            }
       |        }
       |    ]
       |}
       |
       |""".stripMargin
  }

  private def expectedSlackMessage(exportStatusEvent: ExportStatusEvent): String = {
    val successDetails = exportStatusEvent.successDetails
    val failureCause = exportStatusEvent.failureCause
    val exportOutputMessage = if(successDetails.isDefined) {
      s"""\\n*User ID:* ${successDetails.get.userId}\\n*Consignment Reference:* ${successDetails.get.consignmentReference}\\n*Transferring Body Name:* ${successDetails.get.transferringBodyName}"""
    } else if(failureCause.isDefined) s"""\\n*Cause:* ${failureCause.get}""" else """"""

    if (exportStatusEvent.success) {
      s"""{
         |  "blocks" : [ {
         |    "type" : "section",
         |    "text" : {
         |      "type" : "mrkdwn",
         |      "text" : ":white_check_mark: Export *success* on *${exportStatusEvent.environment}!* \\n*Consignment ID:* ${exportStatusEvent.consignmentId}$exportOutputMessage"
         |    }
         |  } ]
         |}""".stripMargin
    } else {
      s"""{
         |  "blocks" : [ {
         |    "type" : "section",
         |    "text" : {
         |      "type" : "mrkdwn",
         |      "text" : ":x: Export *failure* on *${exportStatusEvent.environment}!* \\n*Consignment ID:* ${exportStatusEvent.consignmentId}$exportOutputMessage"
         |    }
         |  } ]
         |}""".stripMargin
    }
  }

  private def expectedSqsMessage(exportStatusEvent: ExportStatusEvent): Option[SqsExpectedMessageDetails] = {
    if (exportStatusEvent.success && exportStatusEvent.successDetails.isDefined) {
      Some(SqsExpectedMessageDetails(exportStatusEvent.successDetails.get, 0))
    } else None
  }

  private def expectedSnsMessage(exportStatusEvent: ExportStatusEvent): Option[String] = {

    if (exportStatusEvent.success && exportStatusEvent.successDetails.isDefined) {
      val successDetails = exportStatusEvent.successDetails.get
      val consignmentRef: String = successDetails.consignmentReference
      val consignmentType: String = successDetails.consignmentType
      val bucket: String = successDetails.exportBucket

      Some(s"""
         | {
         |  "Records": {
         |    "Sns": {
         |      "Message": "{
         |        \\"version\\": \\"1.0.0\\",
         |        \\"timestamp\\": 1661155064747274000,
         |        \\"UUIDs\\": [
         |          {
         |            \\"TDR-UUID\\": \\"45be6508-b693-441d-a0d6-defb3f41c1fb\\"
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
         |""".stripMargin)
    } else None
  }
}
