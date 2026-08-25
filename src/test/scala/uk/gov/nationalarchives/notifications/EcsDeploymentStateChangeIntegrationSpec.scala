package uk.gov.nationalarchives.notifications

class EcsDeploymentStateChangeIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "an ECS deployment state change with a stopped reason",
      input = ecsDeploymentStateChangeInput(
        taskArn = "arn:aws:ecs:eu-west-2:123456789012:task/frontend_prod/1234567890abcdef",
        lastStatus = "STOPPED",
        stoppedReason = Some("Essential container in task exited"),
        containerName = "frontend",
        exitCode = 255,
        containerReason = Some("OutOfMemoryError")
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = expectedSlackMessage(
              taskArn = "arn:aws:ecs:eu-west-2:123456789012:task/frontend_prod/1234567890abcdef",
              lastStatus = "STOPPED",
              stoppedReason = Some("Essential container in task exited"),
              containerName = "frontend",
              exitCode = 255,
              containerReason = Some("OutOfMemoryError")
            ),
            webhookUrl = "/webhook-url"
          )
        )
      )
    ),
    Event(
      description = "an ECS deployment state change without a stopped reason",
      input = ecsDeploymentStateChangeInput(
        taskArn = "arn:aws:ecs:eu-west-2:123456789012:task/backend_prod/abcdef1234567890",
        lastStatus = "FAILED",
        stoppedReason = None,
        containerName = "backend",
        exitCode = 137,
        containerReason = None
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = expectedSlackMessage(
              taskArn = "arn:aws:ecs:eu-west-2:123456789012:task/backend_prod/abcdef1234567890",
              lastStatus = "FAILED",
              stoppedReason = None,
              containerName = "backend",
              exitCode = 137,
              containerReason = None
            ),
            webhookUrl = "/webhook-url"
          )
        )
      )
    )
  )

  private def ecsDeploymentStateChangeInput(
    taskArn: String,
    lastStatus: String,
    stoppedReason: Option[String],
    containerName: String,
    exitCode: Int,
    containerReason: Option[String]
  ): String = {
    val stoppedReasonJson = stoppedReason.map(reason => s"\\\"stoppedReason\\\":\\\"$reason\\\",").getOrElse("")
    val containerReasonJson = containerReason.map(reason => s",\\\"reason\\\":\\\"$reason\\\"").getOrElse("")

    s"""{
       |  "Records": [
       |    {
       |      "Sns": {
       |        "Message": "{\\\"detail-type\\\":\\\"ECS Task State Change\\\",\\\"detail\\\":{\\\"taskArn\\\":\\\"$taskArn\\\",\\\"lastStatus\\\":\\\"$lastStatus\\\",$stoppedReasonJson\\\"containers\\\":[{\\\"name\\\":\\\"$containerName\\\",\\\"exitCode\\\":$exitCode$containerReasonJson}]}}"
       |      }
       |    }
       |  ]
       |}
       |""".stripMargin
  }

  private def expectedSlackMessage(
    taskArn: String,
    lastStatus: String,
    stoppedReason: Option[String],
    containerName: String,
    exitCode: Int,
    containerReason: Option[String]
  ): String = {
    val containerReasonText = containerReason.map(reason => s"Some($reason)").getOrElse("None")
    val containerText = s"EcsTaskContainer($containerName,Some($exitCode),$containerReasonText)"

    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":red_circle: *ECS Deployment State Change Event*\\n*Task*: ...${taskArn.split("task").last}\\n*Container status*: $containerText $lastStatus with exit code $exitCode\\n*StoppedReason*: ${stoppedReason.getOrElse("")}"
       |    }
       |  } ]
       |}
       |""".stripMargin
  }
}
