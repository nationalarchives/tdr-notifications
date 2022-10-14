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

  private def slackMessage(status: String, metricName: String, newStateReason: String, dimensions: String): Option[String] = {
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
       |""".stripMargin.some
  }

  override def events: TableFor8[String, String, Option[String], Option[String], Option[SqsExpectedMessageDetails], Option[String], () => Unit, String] = Table(
    ("description", "input", "emailBody", "slackBody", "sqsMessage", "snsMessage", "stubContext", "slackUrl"),
    (
      "Alarm Test1 with state OK, reason TestReason1, dimensions test1Name - test1Value",
      event("OK", "Test1", "TestReason1", "test1Name", "test1Value"),
      None,
      slackMessage("OK", "Test1", "TestReason1", "test1Name - test1Value"),
      None,
      None,
      () => (),
      "/webhook"
    ),
    (
      "Alarm Test2 with state ALARM, reason TestReason2, dimensions test2Name - test2Value",
      event("ALARM", "Test2", "TestReason2", "test2Name", "test2Value"),
      None,
      slackMessage("ALARM", "Test2", "TestReason2", "test2Name - test2Value"),
      None,
      None,
      () => (),
      "/webhook"
    )
  )
}
