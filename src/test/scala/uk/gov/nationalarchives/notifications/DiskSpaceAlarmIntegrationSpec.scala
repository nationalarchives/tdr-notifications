package uk.gov.nationalarchives.notifications

import cats.implicits._
import org.scalatest.prop.TableFor6
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportSuccessDetails

class DiskSpaceAlarmIntegrationSpec extends LambdaIntegrationSpec {

  private def event(status: String, serverName: String, threshold: Int, newStateReason: String = "") = {
    s"""
       |{
       |    "Records": [
       |        {
       |            "Sns": {
       |                "Message": "{\\"AlarmName\\":\\"tdr-jenkins-disk-space-alarm-mgmt\\",\\"NewStateReason\\": \\"$newStateReason\\",\\"NewStateValue\\":\\"$status\\",\\"Trigger\\":{\\"Dimensions\\":[{\\"value\\":\\"$serverName\\",\\"name\\":\\"server_name\\"}],\\"Threshold\\":$threshold}}"
       |            }
       |        }
       |    ]
       |}
       |
       |""".stripMargin
  }

  private def slackMessage(status: String, serverName: String, threshold: Int, newStateReason: String = ""): Option[String] = {
    if (status == "ALARM") {
      if(newStateReason != "") {
        s"""{
           |  "blocks" : [ {
           |    "type" : "section",
           |    "text" : {
           |      "type" : "mrkdwn",
           |      "text" : ":warning: $serverName is not sending disk space data to Cloudwatch. This is most likely because Jenkins is restarting."
           |    }
           |  },
           |  {
           |			"type": "section",
           |			"text": {
           |				"type": "mrkdwn",
           |				"text": "See <https://grafana.tdr-management.nationalarchives.gov.uk/d/eDVRAnI7z/jenkins-disk-space|this Grafana dashboard> to see the data"
           |			}
           |	}
           |  ]
           |}""".stripMargin.some
      } else {
        s"""{
           |  "blocks" : [ {
           |    "type" : "section",
           |    "text" : {
           |      "type" : "mrkdwn",
           |      "text" : ":warning: $serverName disk space is over $threshold percent"
           |    }
           |  },
           |  {
           |	  "type": "section",
           |			"text": {
           |				"type": "mrkdwn",
           |				"text": "See <https://grafana.tdr-management.nationalarchives.gov.uk/d/eDVRAnI7z/jenkins-disk-space|this Grafana dashboard> to see the data"
           |			}
           |	},
           |  {
           |    "type": "section",
           |      "text": {
           |        "type": "mrkdwn",
           |        "text": "See <https://github.com/nationalarchives/tdr-dev-documentation/blob/master/manual/clear-jenkins-disk-space.md|the dev documentation> for details of how to clear disk space"
           |      }
           |  }
           |  ]
           |}""".stripMargin.some
      }

    } else if(status == "OK") {
      s"""{
         |  "blocks" : [ {
         |    "type" : "section",
         |    "text" : {
         |      "type" : "mrkdwn",
         |      "text" : ":white_check_mark: $serverName disk space is now below $threshold percent"
         |    }
         |  },
         |  {
         |	  "type": "section",
         |			"text": {
         |				"type": "mrkdwn",
         |				"text": "See <https://grafana.tdr-management.nationalarchives.gov.uk/d/eDVRAnI7z/jenkins-disk-space|this Grafana dashboard> to see the data"
         |			}
         |	}
         |  ]
         |}""".stripMargin.some
    } else {
      None
    }
  }

  override def events: TableFor6[String, String, Option[String], Option[String],  Option[SqsExpectedMessageDetails], () => Unit] = Table(
    ("description", "input", "emailBody", "slackBody", "sqsMessage", "stubContext"),
    ("Alarm OK for server Jenkins with threshold 20", event("OK", "Jenkins", 20), None, slackMessage("OK", "Jenkins", 20), None, () => ()),
    ("Alarm OK for server Jenkins with threshold 70", event("OK", "Jenkins", 70), None, slackMessage("OK", "Jenkins", 70), None, () => ()),
    ("Alarm OK for server JenkinsProd with threshold 70", event("OK", "JenkinsProd", 70), None, slackMessage("OK", "JenkinsProd", 70), None, () => ()),
    ("Alarm ALARM for server Jenkins with threshold 20", event("ALARM", "Jenkins", 20), None, slackMessage("ALARM", "Jenkins", 20), None, () => ()),
    ("Alarm ALARM for server Jenkins with threshold 70", event("ALARM", "Jenkins", 70), None, slackMessage("ALARM", "Jenkins", 70), None, () => ()),
    ("Alarm ALARM for server JenkinsProd with threshold 70", event("ALARM", "JenkinsProd", 70), None, slackMessage("ALARM", "JenkinsProd", 70), None, () => ()),
    ("Alarm ALARM for server JenkinsProd with no data points", event("ALARM", "JenkinsProd", 70, "no datapoints were received"), None, slackMessage("ALARM", "JenkinsProd", 70, "no datapoints were received"), None, () => ()),
    ("Alarm ALARM for server Jenkins with no data points", event("ALARM", "Jenkins", 70, "no datapoints were received"), None, slackMessage("ALARM", "Jenkins", 70, "no datapoints were received"), None, () => ())
  )
}
