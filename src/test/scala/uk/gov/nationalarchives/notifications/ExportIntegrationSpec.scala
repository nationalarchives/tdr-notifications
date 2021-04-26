package uk.gov.nationalarchives.notifications

import java.util.UUID

import org.scalatest.prop.TableFor5
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.{ExportStatusEvent, ExportSuccessDetails}

class ExportIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: TableFor5[String, String, Option[String], Option[String], () => ()] = Table(
    ("description", "input", "emailBody", "slackBody", "stubContext"),
    ("a successful export event on intg", exportStatusEventInputText(exportStatus1), None, None, () => ()),
    ("a failed export event on intg", exportStatusEventInputText(exportStatus2), None, Some(expectedSlackMessage(exportStatus2)), () => ()),
    ("a successful export event on staging", exportStatusEventInputText(exportStatus3), None, Some(expectedSlackMessage(exportStatus3)), () => ()),
    ("a failed export event on staging", exportStatusEventInputText(exportStatus4), None, Some(expectedSlackMessage(exportStatus4)), () => ()),
    ("a failed export on intg with no error details", exportStatusEventInputText(exportStatus5), None, Some(expectedSlackMessage(exportStatus5)), () => ()),
    ("a failed export on staging with no error details", exportStatusEventInputText(exportStatus6), None, Some(expectedSlackMessage(exportStatus6)), () => ()),
  )

  private lazy val successDetails = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "tb-body1")
  private lazy val causeOfFailure = "Cause of failure"
  private lazy val exportStatus1 = ExportStatusEvent(UUID.randomUUID(), true, "intg", Some(successDetails), None)
  private lazy val exportStatus2 = ExportStatusEvent(UUID.randomUUID(), false, "intg", None, Some(causeOfFailure))
  private lazy val exportStatus3 = ExportStatusEvent(UUID.randomUUID(), true, "staging", Some(successDetails), None)
  private lazy val exportStatus4 = ExportStatusEvent(UUID.randomUUID(), false, "staging", None, Some(causeOfFailure))
  private lazy val exportStatus5 = ExportStatusEvent(UUID.randomUUID(), false, "intg", None, None)
  private lazy val exportStatus6 = ExportStatusEvent(UUID.randomUUID(), false, "staging", None, None)

  private def exportStatusEventInputText(exportStatusEvent: ExportStatusEvent): String = {
    val successDetails = exportStatusEvent.successDetails
    val failureCause = exportStatusEvent.failureCause
    val exportOutputJson = if(successDetails.isDefined) {
      s""", \\"successDetails\\":{\\"userId\\": \\"${successDetails.get.userId}\\",\\"consignmentReference\\": \\"${successDetails.get.consignmentReference}\\",\\"transferringBodyCode\\": \\"${successDetails.get.transferringBodyCode}\\"}"""
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
      s""":\\nUser ID: ${successDetails.get.userId}\\nConsignment Reference: ${successDetails.get.consignmentReference}\\nTransferring Body Code: ${successDetails.get.transferringBodyCode}"""
    } else if(failureCause.isDefined) s""":\\nCause: ${failureCause.get}""" else """"""

    if (exportStatusEvent.success) {
      s"""{
         |  "blocks" : [ {
         |    "type" : "section",
         |    "text" : {
         |      "type" : "mrkdwn",
         |      "text" : ":white_check_mark: *Export success:* \\n*Consignment ID:* ${exportStatusEvent.consignmentId} \\n*Environment:* ${exportStatusEvent.environment}: \\n$exportOutputMessage"
         |    }
         |  } ]
         |}""".stripMargin
    } else {
      s"""{
         |  "blocks" : [ {
         |    "type" : "section",
         |    "text" : {
         |      "type" : "mrkdwn",
         |      "text" : ":x: *Export failure:* \\n*Consignment ID:* ${exportStatusEvent.consignmentId} \\n*Environment:* ${exportStatusEvent.environment}: \\n$exportOutputMessage"
         |    }
         |  } ]
         |}""".stripMargin
    }
  }
}
