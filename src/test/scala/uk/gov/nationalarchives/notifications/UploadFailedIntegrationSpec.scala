package uk.gov.nationalarchives.notifications

class UploadFailedIntegrationSpec extends LambdaIntegrationSpec {

  private lazy val consignmentId = "c140d49c-93d0-4345-8d71-c97ff28b947e"
  private lazy val userId = "22579624-3eb9-4453-9b41-dd53a58fcfe7"

  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "An upload failed event on prod",
      input = uploadFailedInputString("prod", Some("Mock Body"), Some("MOCK1"), Some("TimeoutError: Request timed out")),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("prod", "Mock Body", "MOCK1", "TimeoutError: Request timed out"),
            webhookUrl = "/webhook-transfers"
          )
        )
      )
    ),
    Event(
      description = "An upload failed event on non-prod without an error message",
      input = uploadFailedInputString("intg", None, None, None),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("intg", "N/A", "N/A", "Not provided"),
            webhookUrl = "/webhook-releases"
          )
        )
      )
    )
  )

  private def slackMessage(environment: String, transferringBody: String, series: String, error: String): String = {
    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":warning: *A user has experienced an Upload Failure*\\n*Environment*: $environment\\n*Consignment Reference*: TDR-2024\\n*Consignment ID*: $consignmentId\\n*Transferring Body*: $transferringBody\\n*Series*: $series\\n*UserID*: $userId\\n*Error Message*: $error"
       |    }
       |  } ]
       |}
       |""".stripMargin
  }

  private def uploadFailedInputString(environment: String, transferringBodyName: Option[String], seriesName: Option[String], errorMessage: Option[String]): String = {
    val transferringBody = transferringBodyName.map(t => s"""\\"$t\\"""").getOrElse("null")
    val series = seriesName.map(s => s"""\\"$s\\"""").getOrElse("null")
    val error = errorMessage.map(e => s"""\\"$e\\"""").getOrElse("null")
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"environment\\":\\"$environment\\",\\"consignmentReference\\":\\"TDR-2024\\",\\"consignmentId\\":\\"$consignmentId\\",\\"transferringBodyName\\":$transferringBody,\\"seriesName\\":$series,\\"userId\\":\\"$userId\\",\\"errorMessage\\":$error}"
       |      }
       |    }
       |  ]}""".stripMargin
  }
}
