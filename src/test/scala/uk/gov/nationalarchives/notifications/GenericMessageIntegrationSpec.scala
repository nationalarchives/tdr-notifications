package uk.gov.nationalarchives.notifications

import cats.implicits.catsSyntaxOptionId
import org.scalatest.prop.TableFor8

class GenericMessageIntegrationSpec extends LambdaIntegrationSpec {

  override def events: TableFor8[String, String, Option[String], Option[String], Option[SqsExpectedMessageDetails], Option[SnsExpectedMessageDetails], () => Unit, String] = {
    val oneMessage: String =
      """ "{\"messages\":[{\"message\":\"A test message\"}]}" """
    val oneMessageText = "A test message"
    val twoMessages: String =
      """ "{\"messages\":[{\"message\":\"A test message\"}, {\"message\":\"A second test message\"}]}" """
    val twoMessagesText = "A test message\\nA second test message"

    Table(
      ("description", "input", "emailBody", "slackBody", "sqsMessage", "snsMessage", "stubContext", "slackUrl"),
      ("one message",
        genericEventInput(oneMessage), None, slackMsg(oneMessageText).some, None, None, () => (), "/webhook"),
      ("two messages",
        genericEventInput(twoMessages), None, slackMsg(twoMessagesText).some, None, None, () => (), "/webhook")
    )
  }

  def genericEventInput(rotationNotification: String): String =
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
