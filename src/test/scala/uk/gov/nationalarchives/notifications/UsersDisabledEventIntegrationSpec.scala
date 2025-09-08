package uk.gov.nationalarchives.notifications

import uk.gov.nationalarchives.notifications.decoders.UsersDisabledEventDecoder.{LogInfo, UsersDisabledEvent}

class UsersDisabledEventIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "Keycloak users disabled in lower environment",
      input = usersDisabledEventNotificationInputString(
        UsersDisabledEvent(
          environment = "test", 
          disabledUsersCount = 1, 
          logInfo = LogInfo(
            logGroupName = "/aws/lambda/test-users-disabled", 
            logStreamName = "2025/01/01/abcde"
          )
        )
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("test", 1, "/aws/lambda/test-users-disabled", "2025/01/01/abcde"),
            webhookUrl = "/webhook-url"
          )
        )
      )
    ),
    Event(
      description = "Keycloak users disabled in prod",
      input = usersDisabledEventNotificationInputString(
        UsersDisabledEvent(
          environment = "prod",
          disabledUsersCount = 1,
          logInfo = LogInfo(
            logGroupName = "/aws/lambda/test-users-disabled",
            logStreamName = "2025/01/01/abcde"
          )
        )
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("prod", 1, "/aws/lambda/test-users-disabled", "2025/01/01/abcde"),
            webhookUrl = "/webhook-tdr"
          )
        )
      )
    ),
  )

  private def slackMessage(environment: String, usersDisabled: Int, logGroupName: String, logStreamName: String): String = {
    val text = s":broom: Keycloak disable users lambda run in $environment. $usersDisabled users disabled.\\n:memo: <https://eu-west-2.console.aws.amazon.com/cloudwatch/home?region=eu-west-2#logsV2:log-groups/log-group$logGroupName/log-events/$logStreamName|View the logs on Cloudwatch>"

    s"""{"blocks":[{"type":"section","text":{"type":"mrkdwn","text":"$text"}}]}"""
  }
  
  def usersDisabledEventNotificationInputString(usersDisabledEvent: UsersDisabledEvent): String = {
    import usersDisabledEvent._
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"environment\\":\\"$environment\\",\\"disabledUsersCount\\":\\"$disabledUsersCount\\",\\"logInfo\\": { \\"logGroupName\\" : \\"${logInfo.logGroupName}\\", \\"logStreamName\\" : \\"${logInfo.logStreamName}\\" } }"
       |      }
       |    }
       |  ]}""".stripMargin
  }
}
