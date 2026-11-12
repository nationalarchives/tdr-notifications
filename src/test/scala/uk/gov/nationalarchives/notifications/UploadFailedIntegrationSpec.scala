package uk.gov.nationalarchives.notifications

class UploadFailedIntegrationSpec extends LambdaIntegrationSpec {

  private lazy val consignmentId = "c140d49c-93d0-4345-8d71-c97ff28b947e"
  private lazy val userId = "22579624-3eb9-4453-9b41-dd53a58fcfe7"

  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "An upload failed event for a standard user on prod",
      input = uploadFailedInputString("prod", isJudgmentUser = false, Some("TimeoutError: Request timed out")),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("prod", "standard", "TimeoutError: Request timed out"),
            webhookUrl = "/webhook-transfers"
          )
        )
      )
    ),
    Event(
      description = "An upload failed event for a judgment user on non-prod without an error message",
      input = uploadFailedInputString("intg", isJudgmentUser = true, None),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("intg", "judgment", "Not provided"),
            webhookUrl = "/webhook-releases"
          )
        )
      )
    )
  )

  private def slackMessage(environment: String, consignmentType: String, error: String): String = {
    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":warning: *A user has experienced an Upload Failure*\\n*Environment*: $environment\\n*Consignment Type*: $consignmentType\\n*Consignment Reference*: TDR-2024\\n*Consignment ID*: $consignmentId\\n*UserID*: $userId\\n*Error*: $error"
       |    }
       |  } ]
       |}
       |""".stripMargin
  }

  private def uploadFailedInputString(environment: String, isJudgmentUser: Boolean, errorMessage: Option[String]): String = {
    val error = errorMessage.map(e => s"""\\"$e\\"""").getOrElse("null")
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"environment\\":\\"$environment\\",\\"consignmentReference\\":\\"TDR-2024\\",\\"consignmentId\\":\\"$consignmentId\\",\\"userId\\":\\"$userId\\",\\"isJudgmentUser\\":$isJudgmentUser,\\"errorMessage\\":$error}"
       |      }
       |    }
       |  ]}""".stripMargin
  }
}
