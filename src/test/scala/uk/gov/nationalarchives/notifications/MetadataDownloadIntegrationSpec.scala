package uk.gov.nationalarchives.notifications

class MetadataDownloadIntegrationSpec extends LambdaIntegrationSpec {

  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "A metadata download event on prod",
      input = metadataDownloadInputString(
        environment = "prod",
        userId = "22579624-3eb9-4453-9b41-dd53a58fcfe7",
        userName = "TNA Username",
        consignmentId = "c140d49c-93d0-4345-8d71-c97ff28b947e",
        consignmentReference = "TDR-2024"
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage(
              environment = "prod",
              userId = "22579624-3eb9-4453-9b41-dd53a58fcfe7",
              userName = "TNA Username",
              consignmentId = "c140d49c-93d0-4345-8d71-c97ff28b947e",
              consignmentReference = "TDR-2024"
            ),
            webhookUrl = "/webhook-admin_action_alert"
          )
        )
      )
    ),
    Event(
      description = "A metadata download event on non-prod",
      input = metadataDownloadInputString(
        environment = "intg",
        userId = "22579624-3eb9-4453-9b41-dd53a58fcfe7",
        userName = "TNA Username",
        consignmentId = "c140d49c-93d0-4345-8d71-c97ff28b947e",
        consignmentReference = "TDR-2024"
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = None
      )
    )
  )

  private def slackMessage(environment: String, userId: String, userName: String, consignmentId: String, consignmentReference: String): String = {
    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":information_source: *Metadata file downloaded*\\n*Environment*: $environment\\n*UserID*: $userId\\n*Username*: $userName\\n*Consignment ID*: $consignmentId\\n*Consignment Reference*: $consignmentReference"
       |    }
       |  } ]
       |}
       |""".stripMargin
  }

  private def metadataDownloadInputString(environment: String, userId: String, userName: String, consignmentId: String, consignmentReference: String): String = {
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"environment\\":\\"$environment\\",\\"userId\\":\\"$userId\\",\\"userName\\":\\"$userName\\",\\"consignmentId\\":\\"$consignmentId\\",\\"consignmentReference\\":\\"$consignmentReference\\"}"
       |      }
       |    }
       |  ]}""".stripMargin
  }
}
