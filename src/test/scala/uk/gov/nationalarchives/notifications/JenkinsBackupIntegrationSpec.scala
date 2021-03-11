package uk.gov.nationalarchives.notifications

import org.scalatest.prop.TableFor5
import uk.gov.nationalarchives.notifications.decoders.SSMMaintenanceDecoder.SSMMaintenanceEvent

class JenkinsBackupIntegrationSpec extends LambdaIntegrationSpec {

  override lazy val events: TableFor5[String, String, Option[String], Option[String], () => ()] = Table(
    ("description", "input", "emailBody", "slackBody", "stubContext"),
    ("a successful Jenkins backup event", maintenanceEventInputText(maintenanceResult1), None, None, () => ()),
    ("a failed Jenkins backup event", maintenanceEventInputText(maintenanceResult2), None, Some(expectedBackupFailureSlackMessage), () => ())
  )

  private lazy val maintenanceResult1: SSMMaintenanceEvent = SSMMaintenanceEvent(true)
  private lazy val maintenanceResult2: SSMMaintenanceEvent = SSMMaintenanceEvent(false)

  private def maintenanceEventInputText(ssmMaintenanceResult: SSMMaintenanceEvent): String = {
    val status = if (ssmMaintenanceResult.success) "SUCCESS" else "FAILED"
    s"""
       |{
       |  "detail": {
       |    "status": "$status"
       |  }
       |}
       |""".stripMargin
  }

  private lazy val expectedBackupFailureSlackMessage: String = {
    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : "The Jenkins backup has failed. Please check the maintenance window in systems manager"
       |    }
       |  } ]
       |}""".stripMargin
  }
}
