package uk.gov.nationalarchives.notifications

class FileCheckFailureIntegrationSpec extends LambdaIntegrationSpec {

  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "A file check failure event on prod",
      input = fileCheckFailureInputString(
        consignmentType = "standard",
        consignmentReference = "TDR-2025-ABC",
        consignmentId = "c2e7e539-0410-4dbf-b96e-1e3871d868ad",
        transferringBodyName = "SomeTransferringBody",
        userId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        environment = "prod"
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("standard", "TDR-2025-ABC", "c2e7e539-0410-4dbf-b96e-1e3871d868ad", "SomeTransferringBody", "a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
            webhookUrl = "/webhook-transfers"
          )
        )
      )
    ),
    Event(
      description = "A file check failure event for judgment on prod",
      input = fileCheckFailureInputString(
        consignmentType = "judgment",
        consignmentReference = "TDR-2025-JDG",
        consignmentId = "c2e7e539-0410-4dbf-b96e-1e3871d868ad",
        transferringBodyName = "SomeTransferringBody",
        userId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        environment = "prod"
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("judgment", "TDR-2025-JDG", "c2e7e539-0410-4dbf-b96e-1e3871d868ad", "SomeTransferringBody", "a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
            webhookUrl = "/webhook-transfers"
          )
        )
      )
    ),
    Event(
      description = "A file check failure event on non-prod",
      input = fileCheckFailureInputString(
        consignmentType = "standard",
        consignmentReference = "TDR-2025-DEF",
        consignmentId = "c2e7e539-0410-4dbf-b96e-1e3871d868ad",
        transferringBodyName = "SomeTransferringBody",
        userId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        environment = "intg"
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("standard", "TDR-2025-DEF", "c2e7e539-0410-4dbf-b96e-1e3871d868ad", "SomeTransferringBody", "a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
            webhookUrl = "/webhook-releases"
          )
        )
      )
    ),
    Event(
      description = "A file check failure event with MOCK transferring body",
      input = fileCheckFailureInputString(
        consignmentType = "standard",
        consignmentReference = "TDR-2025-GHI",
        consignmentId = "c2e7e539-0410-4dbf-b96e-1e3871d868ad",
        transferringBodyName = "Mock 1 Department",
        userId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        environment = "intg"
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = None
      )
    )
  )

  private def slackMessage(consignmentType: String, consignmentReference: String, consignmentId: String, transferringBodyName: String, userId: String): String = {
    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":warning: *A user has experienced a File Check Failure*\\n*Consignment Type*: $consignmentType\\n*Consignment Reference*: $consignmentReference\\n*Consignment ID*: $consignmentId\\n*Transferring Body*: $transferringBodyName\\n*UserID*: $userId"
       |    }
       |  } ]
       |}
       |""".stripMargin
  }

  private def fileCheckFailureInputString(consignmentType: String, consignmentReference: String, consignmentId: String, transferringBodyName: String, userId: String, environment: String): String = {
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"consignmentType\\":\\"$consignmentType\\",\\"consignmentReference\\":\\"$consignmentReference\\",\\"consignmentId\\":\\"$consignmentId\\",\\"transferringBodyName\\":\\"$transferringBodyName\\",\\"userId\\":\\"$userId\\",\\"environment\\":\\"$environment\\"}"
       |      }
       |    }
       |  ]}""".stripMargin
  }
}

