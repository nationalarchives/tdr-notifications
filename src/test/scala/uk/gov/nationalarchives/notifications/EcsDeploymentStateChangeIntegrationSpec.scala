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
        exitCode = Some(255)
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = expectedSlackMessage(
              taskArn = "arn:aws:ecs:eu-west-2:123456789012:task/frontend_prod/1234567890abcdef",
              lastStatus = "STOPPED",
              stoppedReason = Some("Essential container in task exited"),
              containerName = "frontend",
              exitCode = Some(255)
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
        exitCode = Some(137)
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = expectedSlackMessage(
              taskArn = "arn:aws:ecs:eu-west-2:123456789012:task/backend_prod/abcdef1234567890",
              lastStatus = "FAILED",
              stoppedReason = None,
              containerName = "backend",
              exitCode = Some(137)
            ),
            webhookUrl = "/webhook-url"
          )
        )
      )
    ),
    Event(
      description = "an ECS deployment state change without an exit code",
      input = ecsDeploymentStateChangeInput(
        taskArn = "arn:aws:ecs:eu-west-2:123456789012:task/backend_prod/abcdef1234567891",
        lastStatus = "STOPPED",
        stoppedReason = Some("Task stopped by deployment"),
        containerName = "backend",
        exitCode = None
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = expectedSlackMessage(
              taskArn = "arn:aws:ecs:eu-west-2:123456789012:task/backend_prod/abcdef1234567891",
              lastStatus = "STOPPED",
              stoppedReason = Some("Task stopped by deployment"),
              containerName = "backend",
              exitCode = None
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
                                             exitCode: Option[Int]
                                           ): String = {
    val stoppedReasonJson = stoppedReason.map(reason => s"\\\"stoppedReason\\\":\\\"$reason\\\",").getOrElse("")
    val exitCodeJson = exitCode.map(code => s",\\\"exitCode\\\":$code").getOrElse("")

    s"""{
       |  "Records": [
       |    {
       |      "Sns": {
       |        "Message": "{\\\"detail-type\\\":\\\"ECS Task State Change\\\",\\\"detail\\\":{\\\"taskArn\\\":\\\"$taskArn\\\",\\\"lastStatus\\\":\\\"$lastStatus\\\",$stoppedReasonJson\\\"containers\\\":[{\\\"name\\\":\\\"$containerName\\\"$exitCodeJson}]}}"
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
                                    exitCode: Option[Int]
                                  ): String = {
    val exitCodeText = exitCode.getOrElse("Unknown")
    val containerNameText = Option.when(exitCode.isDefined)(containerName).getOrElse("")

    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":red_circle: *ECS Deployment State Change Event*\\n*Task*: ...${taskArn.split("task").last}\\n*Container status*: $containerNameText *$lastStatus* with exit code *$exitCodeText*\\n*Stopped reason*: ${stoppedReason.getOrElse("")}"
       |    }
       |  } ]
       |}
       |""".stripMargin
  }
}
