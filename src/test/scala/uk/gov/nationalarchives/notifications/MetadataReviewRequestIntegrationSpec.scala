package uk.gov.nationalarchives.notifications

import uk.gov.nationalarchives.notifications.decoders.MetadataReviewRequestDecoder.MetadataReviewRequestEvent
import uk.gov.nationalarchives.notifications.messages.EventMessages.GovUKEmailDetails

class MetadataReviewRequestIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "A metadata review DTA request event",
      input = metadataReviewRequestNotificationInputString(
        MetadataReviewRequestEvent(
          environment = "intg",
          transferringBodyName = "SomeTransferringBody",
          consignmentReference = "SomeConsignmentReference",
          consignmentId = "SomeConsignmentId",
          seriesCode = "SomeSeries",
          userId = "SomeUserId",
          userEmail = "test@test.test",
          closedRecords = true,
          totalRecords = 10)
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        govUKEmail = Some(
          GovUKEmailDetails(
            reference = "SomeConsignmentReference",
            templateId = "TestRequestDTATemplateId",
            userEmail = "tdr@nationalarchives.gov.uk",
            personalisation = Map(
              "userEmail" -> "test@test.test",
              "userId" -> "SomeUserId",
              "transferringBodyName" -> "SomeTransferringBody",
              "consignmentId" -> "SomeConsignmentId",
              "consignmentReference" -> "SomeConsignmentReference",
              "seriesCode" -> "SomeSeries"
            )
          )
        ),
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("SomeConsignmentReference", "SomeTransferringBody", "SomeSeries", "YES"),
            webhookUrl = "/webhook-releases"
          )
        )
      )
    ),
    Event(
      description = "A metadata review TB request event",
      input = metadataReviewRequestNotificationInputString(
        MetadataReviewRequestEvent(
          environment = "intg",
          transferringBodyName = "ABCD",
          consignmentReference = "TDR-2024",
          consignmentId = "SomeConsignmentId",
          seriesCode = "1234",
          userId = "SomeUserId",
          userEmail = "test@test.test",
          closedRecords = false,
          totalRecords = 10)
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        govUKEmail = Some(
          GovUKEmailDetails(
            reference = "TDR-2024",
            templateId = "TestRequestTBTemplateId",
            userEmail = "test@test.test",
            personalisation = Map(
              "consignmentReference" -> "TDR-2024",
            )
          )
        ),
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("TDR-2024", "ABCD", "1234", "NO"),
            webhookUrl = "/webhook-releases"
          )
        )
      )
    ),
    Event(
      description = "A metadata review request event from a mock transferring body",
      input = metadataReviewRequestNotificationInputString(
        MetadataReviewRequestEvent(
          environment = "intg",
          transferringBodyName = "Mock 1 Department",
          consignmentReference = "TDR-2024",
          consignmentId = "SomeConsignmentId",
          seriesCode = "1234",
          userId = "SomeUserId",
          userEmail = "test@test.test",
          closedRecords = false,
          totalRecords = 10)
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        govUKEmail = Some(
          GovUKEmailDetails(
            reference = "TDR-2024",
            templateId = "TestRequestTBTemplateId",
            userEmail = "test@test.test",
            personalisation = Map(
              "consignmentReference" -> "TDR-2024",
            )
          )
        ),
        slackMessage = None
      )
    )
  )

  private def slackMessage(consignmentReference: String, transferringBody: String, series: String, closedRecords: String): String = {
    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":warning: *A Metadata Review has been SUBMITTED*\\n*Consignment Reference*: $consignmentReference\\n*Transferring Body*: $transferringBody\\n*Series*: $series\\n*UserID*: SomeUserId\\n*Number of Records*: 10\\n*Closed Records*: $closedRecords"
       |    }
       |  } ]
       |}
       |""".stripMargin
  }

  def metadataReviewRequestNotificationInputString(metadataReviewRequestEvent: MetadataReviewRequestEvent): String = {
    import metadataReviewRequestEvent._
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"environment\\":\\"$environment\\",\\"transferringBodyName\\":\\"$transferringBodyName\\",\\"consignmentReference\\":\\"$consignmentReference\\",\\"consignmentId\\" : \\"$consignmentId\\",\\"seriesCode\\" : \\"$seriesCode\\",\\"userId\\" : \\"$userId\\",\\"userEmail\\" : \\"$userEmail\\",\\"closedRecords\\":$closedRecords,\\"totalRecords\\":$totalRecords}"
       |      }
       |    }
       |  ]}""".stripMargin
  }
}
