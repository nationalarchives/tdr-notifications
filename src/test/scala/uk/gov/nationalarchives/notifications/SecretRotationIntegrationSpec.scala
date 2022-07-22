package uk.gov.nationalarchives.notifications

import cats.implicits.catsSyntaxOptionId
import org.scalatest.prop.TableFor7

class SecretRotationIntegrationSpec extends LambdaIntegrationSpec {


  override def events: TableFor7[String, String, Option[String], Option[String], Option[SqsExpectedMessageDetails], () => Unit, String] = {
    val successfulNotification: String =
      """ "{\"results\":[{\"clientId\":\"successful\",\"success\":true,\"rotationResultErrorMessage\":null}]}" """
    val successfulMessageText = "Client successful has been rotated successfully"
    val failedNotification: String =
      """ "{\"results\":[{\"clientId\":\"failed\",\"success\":false,\"rotationResultErrorMessage\":\"An error\"}]}" """
    val failedMessageText = "Client failed rotation has failed"
    val oneEachNotification: String =
      """ "{\"results\":[{\"clientId\":\"failed\",\"success\":false,\"rotationResultErrorMessage\":\"An error\"},""" +
        """{\"clientId\":\"successful\",\"success\":true,\"rotationResultErrorMessage\":null}]}" """
    val oneEachMessageText = "Client failed rotation has failed\\nClient successful has been rotated successfully"

    Table(
      ("description", "input", "emailBody", "slackBody", "sqsMessage", "stubContext", "slackUrl"),
      ("a successful client notification",
        secretRotationEventInput(successfulNotification), None, slackMsg(successfulMessageText).some, None, () => (), "/webhook"),
      ("a standard transform engine retry event on intg",
        secretRotationEventInput(failedNotification), None, slackMsg(failedMessageText).some, None, () => (), "/webhook"),
      ("a judgment transform engine event on staging",
        secretRotationEventInput(oneEachNotification), None, slackMsg(oneEachMessageText).some, None, () => (), "/webhook")
    )
  }

  def secretRotationEventInput(rotationNotification: String): String =
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": $rotationNotification
       |      }
       |    }
       |  ]}""".stripMargin

  private def slackMsg(message: String): String = {
    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : "$message"
       |    }
       |  } ]
       |}""".stripMargin
  }
}
