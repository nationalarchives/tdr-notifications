package uk.gov.nationalarchives.notifications

import org.scalatest.prop.TableFor8
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.{ExportStatusEvent, ExportSuccessDetails}

import java.util.UUID

class ExportIntegrationSpec extends LambdaIntegrationSpec {

  override lazy val events: TableFor8[String, String, Option[String], Option[String], Option[SqsExpectedMessageDetails], Option[SnsExpectedMessageDetails], () => Unit, String] = Table(
    ("description", "input", "emailBody", "slackBody", "sqsMessage", "snsMessage", "stubContext", "slackUrl"),
    ("a successful standard export event on intg",
      exportStatusEventInputText(intgStandardSuccess), None, None, None, expectedSnsMessage(intgStandardSuccess), () => (), "/webhook-export"),
    ("a successful mock standard export event on intg",
      exportStatusEventInputText(intgStandardSuccessMock), None, None, None, expectedSnsMessage(intgStandardSuccessMock), () => (), "/webhook-export"),
    ("a successful judgment export event on intg",
      exportStatusEventInputText(intgJudgmentSuccess), None, None, None, expectedSnsMessage(intgJudgmentSuccess), () => (), "/webhook-export"),
    ("a successful mock judgment export event on intg",
      exportStatusEventInputText(intgJudgmentSuccessMock), None, None, None, expectedSnsMessage(intgJudgmentSuccessMock), () => (), "/webhook-export"),
    ("a failed export event on intg",
      exportStatusEventInputText(intgFailure), None, Some(expectedSlackMessage(intgFailure)), None, None, () => (), "/webhook-export"),
    ("a successful standard export event on staging",
      exportStatusEventInputText(stagingStandardSuccess), None, Some(expectedSlackMessage(stagingStandardSuccess)), None, expectedSnsMessage(stagingStandardSuccess), () => (), "/webhook-export"),
    ("a successful mock standard export event on staging",
      exportStatusEventInputText(stagingStandardSuccessMock), None, Some(expectedSlackMessage(stagingStandardSuccessMock)), None, None, () => (), "/webhook-export"),
    ("a successful judgment export event on staging",
      exportStatusEventInputText(stagingJudgmentSuccess), None, Some(expectedSlackMessage(stagingJudgmentSuccess)), None, expectedSnsMessage(stagingJudgmentSuccess), () => (), "/webhook-export"),
    ("a successful mock judgment export event on staging",
      exportStatusEventInputText(stagingJudgmentSuccessMock), None, Some(expectedSlackMessage(stagingJudgmentSuccessMock)), None, None, () => (), "/webhook-export"),
    ("a failed export event on staging",
      exportStatusEventInputText(stagingFailure), None, Some(expectedSlackMessage(stagingFailure)), None, None, () => (), "/webhook-export"),
    ("a failed export on intg with no error details",
      exportStatusEventInputText(intgFailureNoError), None, Some(expectedSlackMessage(intgFailureNoError)), None, None, () => (), "/webhook-export"),
    ("a failed export on staging with no error details",
      exportStatusEventInputText(stagingFailureNoError), None, Some(expectedSlackMessage(stagingFailureNoError)), None, None, () => (), "/webhook-export"),
    ("a successful standard export event on prod",
      exportStatusEventInputText(prodStandardSuccess), None, Some(expectedSlackMessage(prodStandardSuccess)), None, expectedSnsMessage(prodStandardSuccess), () => (), "/webhook-standard"),
    ("a successful mock standard export event on prod",
      exportStatusEventInputText(prodStandardSuccessMock), None, Some(expectedSlackMessage(prodStandardSuccessMock)), None, None, () => (), "/webhook-standard"),
    ("a failed standard export event on prod",
      exportStatusEventInputText(prodFailure), None, Some(expectedSlackMessage(prodFailure)), None, None, () => (), "/webhook-export"),
    ("a successful judgment export on prod",
      exportStatusEventInputText(prodJudgmentSuccess), None, Some(expectedSlackMessage(prodJudgmentSuccess)), None, expectedSnsMessage(prodJudgmentSuccess), () => (), "/webhook-judgment"),
    ("a successful mock judgment export on prod",
      exportStatusEventInputText(prodJudgmentSuccessMock), None, Some(expectedSlackMessage(prodJudgmentSuccessMock)), None, None, () => (), "/webhook-judgment"),
  )

  private lazy val successDetailsStandard = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "tb-body1", "standard", "export-bucket")
  private lazy val successDetailsJudgment = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "tb-body1", "judgment", "export-bucket")
  private lazy val successDetailsStandardMockBody = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "Mock 1 Department", "standard", "export-bucket")
  private lazy val successDetailsJudgmentMockBody = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "Mock 1 Department", "judgment", "export-bucket")
  private lazy val causeOfFailure = "Cause of failure"

  private lazy val intgStandardSuccess = ExportStatusEvent(UUID.randomUUID(), success = true, environment = "intg", successDetails = Some(successDetailsStandard), failureCause = None)
  private lazy val intgStandardSuccessMock = ExportStatusEvent(UUID.randomUUID(), success = true, environment = "intg", successDetails = Some(successDetailsStandardMockBody), failureCause = None)
  private lazy val intgJudgmentSuccess = ExportStatusEvent(UUID.randomUUID(), success = true, "intg", Some(successDetailsJudgment), None)
  private lazy val intgJudgmentSuccessMock = ExportStatusEvent(UUID.randomUUID(), success = true, "intg", Some(successDetailsJudgmentMockBody), None)
  private lazy val intgFailure = ExportStatusEvent(UUID.randomUUID(), success = false, "intg", None, Some(causeOfFailure))
  private lazy val intgFailureNoError = ExportStatusEvent(UUID.randomUUID(), success = false, "intg", None, None)

  private lazy val stagingStandardSuccess = ExportStatusEvent(UUID.randomUUID(), success = true, "staging", Some(successDetailsStandard), None)
  private lazy val stagingStandardSuccessMock = ExportStatusEvent(UUID.randomUUID(), success = true, "staging", Some(successDetailsStandardMockBody), None)
  private lazy val stagingJudgmentSuccess = ExportStatusEvent(UUID.randomUUID(), success = true, "staging", Some(successDetailsJudgment), None)
  private lazy val stagingJudgmentSuccessMock = ExportStatusEvent(UUID.randomUUID(), success = true, "staging", Some(successDetailsJudgmentMockBody), None)
  private lazy val stagingFailure = ExportStatusEvent(UUID.randomUUID(), success = false, "staging", None, Some(causeOfFailure))
  private lazy val stagingFailureNoError = ExportStatusEvent(UUID.randomUUID(), success = false, "staging", None, None)

  private lazy val prodStandardSuccess = ExportStatusEvent(UUID.randomUUID(), success = true, "prod", Some(successDetailsStandard), None)
  private lazy val prodStandardSuccessMock = ExportStatusEvent(UUID.randomUUID(), success = true, "prod", Some(successDetailsStandardMockBody), None)
  private lazy val prodJudgmentSuccess = ExportStatusEvent(UUID.randomUUID(), success = true, "prod", Some(successDetailsJudgment), None)
  private lazy val prodJudgmentSuccessMock = ExportStatusEvent(UUID.randomUUID(), success = true, "prod", Some(successDetailsJudgmentMockBody), None)
  private lazy val prodFailure = ExportStatusEvent(UUID.randomUUID(), success = false, "prod", None, Some(causeOfFailure))

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

  private def expectedSnsMessage(exportStatusEvent: ExportStatusEvent): Option[SnsExpectedMessageDetails] = {
    if (exportStatusEvent.success && exportStatusEvent.successDetails.isDefined) {
      val successDetails = exportStatusEvent.successDetails.get
      val consignmentRef: String = successDetails.consignmentReference
      val consignmentType: String = successDetails.consignmentType
      val bucket: String = successDetails.exportBucket
      val environment: String = exportStatusEvent.environment

      Some(SnsExpectedMessageDetails(consignmentRef, consignmentType, bucket, environment))

    } else None
  }
}
