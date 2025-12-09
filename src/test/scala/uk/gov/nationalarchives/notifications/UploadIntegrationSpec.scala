package uk.gov.nationalarchives.notifications

import uk.gov.nationalarchives.notifications.decoders.UploadEventDecoder.UploadEvent
import uk.gov.nationalarchives.notifications.messages.EventMessages.GovUKEmailDetails

class UploadIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "A upload complete event",
      input = uploadNotificationInputString(
        UploadEvent(
          transferringBodyName = "SomeTransferringBody",
          consignmentReference = "SomeConsignmentReference",
          consignmentId = "SomeConsignmentId",
          userId = "SomeUserId",
          userEmail = "test@test.test",
          status = "Completed",
          assetSource = "SomeSource",
          environment = "intg"
        )
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        govUKEmail = Some(
          GovUKEmailDetails(
            reference = "SomeConsignmentReference-SomeUserId",
            templateId = "TestUploadCompleteTemplateId",
            userEmail = "test@test.test",
            personalisation = Map(
              "userEmail" -> "test@test.test",
              "userId" -> "SomeUserId",
              "transferringBodyName" -> "SomeTransferringBody",
              "consignmentId" -> "SomeConsignmentId",
              "consignmentReference" -> "SomeConsignmentReference",
              "status" -> "Completed"
            )
          )
        )
      )
    ),
    Event(
      description = "An upload failed event",
      input = uploadNotificationInputString(
        UploadEvent(
          transferringBodyName = "SomeTransferringBody",
          consignmentReference = "SomeConsignmentReference",
          consignmentId = "SomeConsignmentId",
          status = "Failed",
          userId = "SomeUserId",
          userEmail = "test@test.test",
          assetSource = "SomeSource",
          environment = "prod"
        )
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        govUKEmail = Some(
          GovUKEmailDetails(
            reference = "SomeConsignmentReference-SomeUserId",
            templateId = "TestUploadFailedTemplateId",
            userEmail = "test@test.test",
            personalisation = Map(
              "userEmail" -> "test@test.test",
              "userId" -> "SomeUserId",
              "transferringBodyName" -> "SomeTransferringBody",
              "consignmentId" -> "SomeConsignmentId",
              "consignmentReference" -> "SomeConsignmentReference",
              "status" -> "Failed"
            )
          )
        ),
        slackMessage = Some(
          SlackMessage(
            body = slackMessage(UploadEvent(
              transferringBodyName = "SomeTransferringBody",
              consignmentReference = "SomeConsignmentReference",
              consignmentId = "SomeConsignmentId",
              status = "Failed",
              userId = "SomeUserId",
              userEmail = "test@test.com",
              assetSource = "SomeSource",
              environment = "prod")),
            webhookUrl = "/webhook-tdr"
          )
        )
      )
    ),
    Event(
      description = "A upload complete event with MOCK transferring body",
      input = uploadNotificationInputString(
        UploadEvent(
          transferringBodyName = "MOCK123",
          consignmentReference = "SomeConsignmentReference",
          consignmentId = "SomeConsignmentId",
          userId = "SomeUserId",
          userEmail = "test@test.test",
          status = "Completed",
          assetSource = "SomeSource",
          environment = "intg"
        )
      ),
      expectedOutput = ExpectedOutput(
        govUKEmail = None,
        slackMessage = None
      )
    ),
    Event(
      description = "An upload failed event with MOCK transferring body",
      input = uploadNotificationInputString(
        UploadEvent(
          transferringBodyName = "MOCK123",
          consignmentReference = "SomeConsignmentReference",
          consignmentId = "SomeConsignmentId",
          status = "Failed",
          userId = "SomeUserId",
          userEmail = "test@test.test",
          assetSource = "SomeSource",
          environment = "prod"
        )
      ),
      expectedOutput = ExpectedOutput(
        govUKEmail = None,
        slackMessage = None
      )
    )
  )

  def uploadNotificationInputString(uploadEvent: UploadEvent): String = {
    import uploadEvent._
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"transferringBodyName\\":\\"$transferringBodyName\\",\\"consignmentReference\\":\\"$consignmentReference\\",\\"consignmentId\\" : \\"$consignmentId\\",\\"status\\" : \\"$status\\",\\"userId\\" : \\"$userId\\",\\"userEmail\\" : \\"$userEmail\\",\\"assetSource\\" : \\"$assetSource\\",\\"environment\\" : \\"$environment\\"}"
       |      }
       |    }
       |  ]}""".stripMargin
  }

  private def slackMessage(uploadEvent: UploadEvent): String = {
    val messageList = List(
      s":warning: *Transfer Upload ${uploadEvent.status} ${uploadEvent.environment}*",
      s"*Asset Source*: ${uploadEvent.assetSource}",
      s"*Consignment Reference*: ${uploadEvent.consignmentReference}",
      s"*Consignment Id*: ${uploadEvent.consignmentId}",
      s"*User Id*: ${uploadEvent.userId}",
    )
    s"""{
       |  "blocks": [
       |    {
       |      "type": "section",
       |      "text": {
       |        "type": "mrkdwn",
       |        "text": "${messageList.mkString("\\n")}"
       |      }
       |    }
       |  ]
       |}
       |""".stripMargin
  }
}
