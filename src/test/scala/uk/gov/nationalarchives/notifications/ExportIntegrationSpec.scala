package uk.gov.nationalarchives.notifications

import java.util.UUID

import org.scalatest.prop.TableFor6
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.{ExportStatusEvent, ExportSuccessDetails}

class ExportIntegrationSpec extends LambdaIntegrationSpec {

  override lazy val events: TableFor6[String, String, Option[String], Option[String], Option[SqsExpectedMessageDetails], () => ()] = Table(
    ("description", "input", "emailBody", "slackBody", "sqsMessage", "stubContext"),
    ("a successful standard export event on intg",
      exportStatusEventInputText(exportStatus1), None, None, None, () => ()),
    ("a successful judgment export event on intg",
      exportStatusEventInputText(exportStatus2), None, None, expectedSqsMessage(exportStatus2), () => ()),
    ("a successful judgment export event using a mock transferring body on intg",
      exportStatusEventInputText(exportStatus3), None, None, None, () => ()),
    ("a failed export event on intg",
      exportStatusEventInputText(exportStatus4), None, Some(expectedSlackMessage(exportStatus4)), None, () => ()),
    ("a successful standard export event on staging",
      exportStatusEventInputText(exportStatus5), None, Some(expectedSlackMessage(exportStatus5)), None, () => ()),
    ("a successful judgment export event on staging",
      exportStatusEventInputText(exportStatus6), None, Some(expectedSlackMessage(exportStatus6)), expectedSqsMessage(exportStatus6), () => ()),
    ("a successful judgment export event using a mock transferring body on staging",
      exportStatusEventInputText(exportStatus7), None, Some(expectedSlackMessage(exportStatus7)), None, () => ()),
    ("a failed export event on staging",
      exportStatusEventInputText(exportStatus8), None, Some(expectedSlackMessage(exportStatus8)), None, () => ()),
    ("a failed export on intg with no error details",
      exportStatusEventInputText(exportStatus9), None, Some(expectedSlackMessage(exportStatus9)), None, () => ()),
    ("a failed export on staging with no error details",
      exportStatusEventInputText(exportStatus10), None, Some(expectedSlackMessage(exportStatus10)), None, () => ())
  )

  private lazy val successDetailsStandard = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "tb-body1", "standard", "export-bucket")
  private lazy val successDetailsJudgment = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "tb-body1", "judgment", "export-bucket")
  private lazy val successDetailsJudgmentMockBody = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "MOCK1 Department", "judgment", "export-bucket")
  private lazy val causeOfFailure = "Cause of failure"
  private lazy val exportStatus1 = ExportStatusEvent(UUID.randomUUID(), true, "intg", Some(successDetailsStandard), None)
  private lazy val exportStatus2 = ExportStatusEvent(UUID.randomUUID(), true, "intg", Some(successDetailsJudgment), None)
  private lazy val exportStatus3 = ExportStatusEvent(UUID.randomUUID(), true, "intg", Some(successDetailsJudgmentMockBody), None)
  private lazy val exportStatus4 = ExportStatusEvent(UUID.randomUUID(), false, "intg", None, Some(causeOfFailure))
  private lazy val exportStatus5 = ExportStatusEvent(UUID.randomUUID(), true, "staging", Some(successDetailsStandard), None)
  private lazy val exportStatus6 = ExportStatusEvent(UUID.randomUUID(), true, "staging", Some(successDetailsJudgment), None)
  private lazy val exportStatus7 = ExportStatusEvent(UUID.randomUUID(), true, "staging", Some(successDetailsJudgmentMockBody), None)
  private lazy val exportStatus8 = ExportStatusEvent(UUID.randomUUID(), false, "staging", None, Some(causeOfFailure))
  private lazy val exportStatus9 = ExportStatusEvent(UUID.randomUUID(), false, "intg", None, None)
  private lazy val exportStatus10 = ExportStatusEvent(UUID.randomUUID(), false, "staging", None, None)

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
       |                "Message": "{\\"success\\":${exportStatusEvent.success},\\"consignmentId\\":\\"${exportStatusEvent.consignmentId}\\", \\"environment\\": \\"${exportStatusEvent.environment}\\"${exportOutputJson}}"
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
}
