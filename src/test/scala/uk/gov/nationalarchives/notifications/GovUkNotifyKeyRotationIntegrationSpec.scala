package uk.gov.nationalarchives.notifications

import cats.implicits.catsSyntaxOptionId
import org.scalatest.prop.TableFor7
import uk.gov.nationalarchives.notifications.decoders.GovUkNotifyKeyRotationDecoder.{Detail, GovUkNotifyKeyRotationEvent}

class GovUkNotifyKeyRotationIntegrationSpec extends LambdaIntegrationSpec {

  override lazy val events: TableFor7[String, String, Option[String], Option[String], Option[SqsExpectedMessageDetails], () => Unit, String] = Table(
    ("description", "input", "emailBody", "slackBody", "sqsMessage", "stubContext", "slackUrl"),
    ("a GovUk key rotation event on intg",
      keyRotationEventInputText(intgRotationEvent), None, expectedSlackMessage(intgRotationEvent), None, () => (), "/webhook"),
    ("a GovUk key rotation event on staging",
      keyRotationEventInputText(stagingRotationEvent), None, expectedSlackMessage(stagingRotationEvent), None, () => (), "/webhook"),
    ("a GovUk key rotation event on prod",
      keyRotationEventInputText(prodRotationEvent), None, expectedSlackMessage(prodRotationEvent), None, () => (), "/webhook")
  )

  private lazy val intgRotationEvent = GovUkNotifyKeyRotationEvent(Detail("/intg/parameter/name", "No change notification message"))
  private lazy val stagingRotationEvent = GovUkNotifyKeyRotationEvent(Detail("/staging/parameter/name", "No change notification message"))
  private lazy val prodRotationEvent = GovUkNotifyKeyRotationEvent(Detail("/prod/parameter/name", "No change notification message"))

  private def keyRotationEventInputText(keyRotationEvent: GovUkNotifyKeyRotationEvent): String = {
    val parameterName = keyRotationEvent.detail.`parameter-name`
    val reason = keyRotationEvent.detail.`action-reason`

    s"""
       |{
       |  "Records": [
       |    {
       |      "Sns": {
       |        "Message": "{\\"detail\\":{\\"parameter-name\\":\\"$parameterName\\",\\"action-reason\\":\\"$reason\\"}}"
       |      }
       |    }
       |  ]
       |}
       |""".stripMargin
  }

  private def expectedSlackMessage(rotationEvent: GovUkNotifyKeyRotationEvent): Option[String] = {
    val ssmParameter: String = rotationEvent.detail.`parameter-name`
    val reason: String = rotationEvent.detail.`action-reason`

    s"""{
       |  "blocks": [
       |    {
       |      "type": "section",
       |      "text": {
       |        "type": "mrkdwn",
       |        "text": "*Rotate GOVUK Notify API Key*\\n*$ssmParameter*: $reason"
       |      }
       |    }
       |  ]
       |}
       |""".stripMargin.some
  }
}


