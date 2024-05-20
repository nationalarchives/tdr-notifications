package uk.gov.nationalarchives.notifications

import cats.implicits.catsSyntaxOptionId
import org.scalatest.prop.TableFor8

class GenericMessageIntegrationSpec extends LambdaIntegrationSpec {

  override def events: Seq[Event] = {
    val oneMessage: String =
      """ "{\"messages\":[{\"message\":\"A test message\"}]}" """
    val oneMessageText = "A test message"
    val twoMessages: String =
      """ "{\"messages\":[{\"message\":\"A test message\"}, {\"message\":\"A second test message\"}]}" """
    val twoMessagesText = "A test message\\nA second test message"

    Seq(
      Event(
        description = "one message",
        input = genericEventInput(oneMessage),
        expectedOutput = ExpectedOutput(
          slackMessage = Some(SlackMessage(slackMsg(oneMessageText), "/webhook"))
        )
      ),
      Event(
        description = "two messages",
        input = genericEventInput(twoMessages),
        expectedOutput = ExpectedOutput(
          slackMessage = Some(SlackMessage(slackMsg(twoMessagesText), "/webhook"))
        )
      )
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
