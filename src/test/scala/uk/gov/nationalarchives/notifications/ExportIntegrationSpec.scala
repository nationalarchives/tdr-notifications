package uk.gov.nationalarchives.notifications

import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.{ExportStatusEvent, ExportSuccessDetails}

import java.util.UUID

class ExportIntegrationSpec extends LambdaIntegrationSpec {

  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "a successful standard export event on intg",
      input = exportStatusEventInputText(intgStandardSuccess),
      expectedOutput = ExpectedOutput(
        snsMessage = expectedSnsMessage(intgStandardSuccess)
      )
    ),
    Event(
      description = "a successful mock standard export event on intg",
      input = exportStatusEventInputText(intgStandardSuccessMock),
      expectedOutput = ExpectedOutput(
        snsMessage = expectedSnsMessage(intgStandardSuccessMock)
      )
    ),
    Event(
      description = "a successful judgment export event on intg",
      input = exportStatusEventInputText(intgJudgmentSuccess),
      expectedOutput = ExpectedOutput(
        snsMessage = expectedSnsMessage(intgJudgmentSuccess)
      )
    ),
    Event(
      description = "a successful mock judgment export event on intg",
      input = exportStatusEventInputText(intgJudgmentSuccessMock),
      expectedOutput = ExpectedOutput(
        snsMessage = expectedSnsMessage(intgJudgmentSuccessMock)
      )
    ),
    Event(
      description = "a failed export event on intg",
      input = exportStatusEventInputText(intgFailure),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(intgFailure), webhookUrl = "/webhook-export"))
      )
    ),
    Event(
      description = "a successful standard export event on staging",
      input = exportStatusEventInputText(stagingStandardSuccess),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(stagingStandardSuccess), webhookUrl = "/webhook-export")),
        snsMessage = expectedSnsMessage(stagingStandardSuccess)
      )
    ),
    Event(
      description = "a successful mock standard export event on staging",
      input = exportStatusEventInputText(stagingStandardSuccessMock),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(stagingStandardSuccessMock), webhookUrl = "/webhook-export"))
      )
    ),
    Event(
      description = "a successful judgment export event on staging",
      input = exportStatusEventInputText(stagingJudgmentSuccess),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(stagingJudgmentSuccess), webhookUrl = "/webhook-export")),
        snsMessage = expectedSnsMessage(stagingJudgmentSuccess)
      )
    ),
    Event(
      description = "a successful mock judgment export event on staging",
      input = exportStatusEventInputText(stagingJudgmentSuccessMock),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(stagingJudgmentSuccessMock), webhookUrl = "/webhook-export"))
      )
    ),
    Event(
      description = "a failed export event on staging",
      input = exportStatusEventInputText(stagingFailure),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(stagingFailure), webhookUrl = "/webhook-export"))
      )
    ),
    Event(
      description = "a failed export on intg with no error details",
      input = exportStatusEventInputText(intgFailureNoError),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(intgFailureNoError), webhookUrl = "/webhook-export"))
      )
    ),
    Event(
      description = "a failed export on staging with no error details",
      input = exportStatusEventInputText(stagingFailureNoError),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(stagingFailureNoError), webhookUrl = "/webhook-export"))
      )
    ),
    Event(
      description = "a successful standard export event on prod",
      input = exportStatusEventInputText(prodStandardSuccess),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(prodStandardSuccess), webhookUrl = "/webhook-standard")),
        snsMessage = expectedSnsMessage(prodStandardSuccess)
      )
    ),
    Event(
      description = "a successful mock standard export event on prod",
      input = exportStatusEventInputText(prodStandardSuccessMock),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(prodStandardSuccessMock), webhookUrl = "/webhook-standard"))
      )
    ),
    Event(
      description = "a failed standard export event on prod",
      input = exportStatusEventInputText(prodFailure),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(prodFailure), webhookUrl = "/webhook-export"))
      )
    ),
    Event(
      description = "a failed export event on prod",
      input = exportStatusEventInputText(prodFailure),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(prodFailure), webhookUrl = "/webhook-tdr"))
      )
    ),
    Event(
      description = "a successful judgment export on prod",
      input = exportStatusEventInputText(prodJudgmentSuccess),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(prodJudgmentSuccess), webhookUrl = "/webhook-judgment")),
        snsMessage = expectedSnsMessage(prodJudgmentSuccess)
      )
    ),
    Event(
      description = "a successful mock judgment export on prod",
      input = exportStatusEventInputText(prodJudgmentSuccessMock),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackMessage(prodJudgmentSuccessMock), webhookUrl = "/webhook-judgment"))
      )
    )
  )

  private lazy val successDetailsStandard = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "tb-body1", "series-id", "standard", "export-bucket")
  private lazy val successDetailsJudgment = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "tb-body1", "", "judgment", "export-bucket")
  private lazy val successDetailsStandardMockBody = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "Mock 1 Department", "series-id", "standard", "export-bucket")
  private lazy val successDetailsJudgmentMockBody = ExportSuccessDetails(UUID.randomUUID(), "consignmentRef1", "Mock 1 Department", "", "judgment", "export-bucket")
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
      s""", \\"successDetails\\":{\\"userId\\": \\"${sd.userId}\\",\\"consignmentReference\\": \\"${sd.consignmentReference}\\",\\"transferringBodyName\\": \\"${sd.transferringBodyName}\\", \\"series\\": \\"${sd.series}\\", \\"consignmentType\\": \\"${sd.consignmentType}\\", \\"exportBucket\\": \\"${sd.exportBucket}\\"}"""
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
      val transferringBody: String = successDetails.transferringBodyName
      val series: String = successDetails.series
      val bucket: String = successDetails.exportBucket
      val environment: String = exportStatusEvent.environment

      Some(SnsExpectedMessageDetails(consignmentRef, consignmentType, transferringBody, series, bucket, environment))

    } else None
  }
}
