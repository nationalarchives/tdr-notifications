package uk.gov.nationalarchives.notifications

class BackendCheckFailureIntegrationSpec extends LambdaIntegrationSpec {

  private lazy val consignmentId = "c2e7e539-0410-4dbf-b96e-1e3871d868ad"

  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "A backend check failure event on prod",
      input = backendCheckFailureInputString(
        consignmentId = consignmentId,
        environment = "prod",
        failureCause = "Some failure cause",
        backEndChecksProcess = "SomeProcess"
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage(consignmentId, "prod", "Some failure cause", "SomeProcess"),
            webhookUrl = "/webhook-transfers"
          )
        )
      )
    ),
    Event(
      description = "A backend check failure event on non-prod",
      input = backendCheckFailureInputString(
        consignmentId = consignmentId,
        environment = "intg",
        failureCause = "Some failure cause",
        backEndChecksProcess = "SomeProcess"
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage(consignmentId, "intg", "Some failure cause", "SomeProcess"),
            webhookUrl = "/webhook-releases"
          )
        )
      )
    ),
    Event(
      description = "A backend check failure event on staging",
      input = backendCheckFailureInputString(
        consignmentId = consignmentId,
        environment = "staging",
        failureCause = "Some failure cause",
        backEndChecksProcess = "SomeProcess"
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage(consignmentId, "staging", "Some failure cause", "SomeProcess"),
            webhookUrl = "/webhook-releases"
          )
        )
      )
    )
  )

  private def slackMessage(consignmentId: String, environment: String, failureCause: String, backEndChecksProcess: String): String = {
    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":warning: *A user has experienced a step function Backend File Check Failure*\\n*Consignment ID*: $consignmentId\\n*Environment*: $environment\\n*Failure Cause*: $failureCause\\n*Backend Checks Process*: $backEndChecksProcess"
       |    }
       |  } ]
       |}
       |""".stripMargin
  }

  private def backendCheckFailureInputString(consignmentId: String, environment: String, failureCause: String, backEndChecksProcess: String): String = {
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"consignmentId\\":\\"$consignmentId\\",\\"environment\\":\\"$environment\\",\\"failureCause\\":\\"$failureCause\\",\\"backEndChecksProcess\\":\\"$backEndChecksProcess\\"}"
       |      }
       |    }
       |  ]}""".stripMargin
  }
}

