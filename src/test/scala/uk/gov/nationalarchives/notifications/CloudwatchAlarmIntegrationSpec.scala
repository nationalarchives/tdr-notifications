package uk.gov.nationalarchives.notifications

import cats.syntax.all._
import org.scalatest.prop.TableFor8

class CloudwatchAlarmIntegrationSpec extends LambdaIntegrationSpec {
  private def event(status: String, metricName: String, newStateReason: String, dimensionName: String, dimensionValue: String): String = {
    s"""
       |{
       |    "Records": [
       |        {
       |            "Sns": {
       |                "Message": "{\\"NewStateReason\\": \\"$newStateReason\\",\\"NewStateValue\\":\\"$status\\",\\"Trigger\\":{\\"MetricName\\": \\"$metricName\\", \\"Dimensions\\":[{\\"value\\":\\"$dimensionValue\\",\\"name\\":\\"$dimensionName\\"}]}}"
       |            }
       |        }
       |    ]
       |}
       |
       |""".stripMargin
  }

  private def slackMessage(status: String, metricName: String, newStateReason: String, dimensions: String): String = {
    s"""{
       |  "blocks": [
       |    {
       |      "type": "section",
       |      "text": {
       |        "type": "mrkdwn",
       |        "text": "*Cloudwatch Alarms*\\nAlarm state $status\\nAlarm triggered by $metricName\\nReason: $newStateReason\\n\\n*Dimensions affected*\\n$dimensions"
       |      }
       |    }
       |  ]
       |}
       |""".stripMargin
  }

  override def events: Seq[Event] = Seq(
    Event(
      description = "Alarm Test1 with state OK, reason TestReason1, dimensions test1Name - test1Value",
      input = event("OK", "Test1", "TestReason1", "test1Name", "test1Value"),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = slackMessage("OK", "Test1", "TestReason1", "test1Name - test1Value"), webhookUrl = "/webhook-url"))
      )
    ),
    Event(
      description = "Alarm Test2 with state ALARM, reason TestReason2, dimensions test2Name - test2Value",
      input = event("ALARM", "Test2", "TestReason2", "test2Name", "test2Value"),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(slackMessage("ALARM", "Test2", "TestReason2", "test2Name - test2Value"), webhookUrl = "/webhook-url"))
      )
    )
  )
}
