package uk.gov.nationalarchives.notifications

import uk.gov.nationalarchives.notifications.decoders.MetadataReviewSubmittedDecoder.MetadataReviewSubmittedEvent
import uk.gov.nationalarchives.notifications.messages.EventMessages.GovUKEmailDetails

class MetadataReviewSubmittedIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "An approved metadata review submitted event",
      input = metadataReviewSubmittedNotificationInputString(
        MetadataReviewSubmittedEvent(
          environment = "intg",
          consignmentReference = "SomeConsignmentReference",
          urlLink = "example.com",
          userEmail = "email@mail.com",
          status = "Completed",
          transferringBodyName = "SomeTransferringBody",
          seriesCode = "SomeSeries",
          userId = "SomeUserId",
          closedRecords = true,
          totalRecords = 10)
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        govUKEmail = Some(
          GovUKEmailDetails(
            reference = "SomeConsignmentReference",
            templateId = "TestApprovedTemplateId",
            userEmail = "email@mail.com",
            personalisation = Map(
              "urlLink" -> "example.com",
              "consignmentReference" -> "SomeConsignmentReference",
            )
          )
        ),
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("APPROVED", "SomeTransferringBody", "SomeSeries", "YES"),
            webhookUrl = "/webhook-releases"
          )
        )
      )
    ),
    Event(
      description = "A rejected metadata review submitted event",
      input = metadataReviewSubmittedNotificationInputString(
        MetadataReviewSubmittedEvent(
          environment = "intg",
          consignmentReference = "SomeConsignmentReference",
          urlLink = "example.com",
          userEmail = "email@mail.com",
          status = "CompletedWithIssues",
          transferringBodyName = "ABCD",
          seriesCode = "1234",
          userId = "SomeUserId",
          closedRecords = false,
          totalRecords = 10)
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        govUKEmail = Some(
          GovUKEmailDetails(
            reference = "SomeConsignmentReference",
            templateId = "TestRejectedTemplateId",
            userEmail = "email@mail.com",
            personalisation = Map(
              "urlLink" -> "example.com",
              "consignmentReference" -> "SomeConsignmentReference",
            )
          )
        ),
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("REJECTED", "ABCD", "1234", "NO"),
            webhookUrl = "/webhook-releases"
          )
        )
      )
    )
  )

  private def slackMessage(status: String, transferringBody: String, series: String, closedRecords: String): String = {
    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":warning: *A Metadata Review has been $status*\\n*Consignment Reference*: SomeConsignmentReference\\n*Transferring Body*: $transferringBody\\n*Series*: $series\\n*UserID*: SomeUserId\\n*Number of Records*: 10\\n*Closed Records*: $closedRecords"
       |    }
       |  } ]
       |}
       |""".stripMargin
  }

  def metadataReviewSubmittedNotificationInputString(metadataReviewSubmittedEvent: MetadataReviewSubmittedEvent): String = {
    import metadataReviewSubmittedEvent._
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"environment\\":\\"$environment\\",\\"consignmentReference\\":\\"$consignmentReference\\",\\"urlLink\\" : \\"$urlLink\\",\\"userEmail\\" : \\"$userEmail\\",\\"status\\" : \\"$status\\",\\"transferringBodyName\\":\\"$transferringBodyName\\",\\"seriesCode\\":\\"$seriesCode\\",\\"userId\\":\\"$userId\\",\\"closedRecords\\":$closedRecords,\\"totalRecords\\":$totalRecords}"
       |      }
       |    }
       |  ]}""".stripMargin
  }
}
