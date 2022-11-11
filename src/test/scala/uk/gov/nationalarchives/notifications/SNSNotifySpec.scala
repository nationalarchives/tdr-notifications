package uk.gov.nationalarchives.notifications

import cats.implicits.catsSyntaxOptionId
import org.scalatest.prop.TableFor8
import uk.gov.nationalarchives.notifications.decoders.SNSNotifyDecoder.{Detail, SNSNotifyEvent}

class SNSNotifySpec extends LambdaIntegrationSpec {

  override lazy val events: TableFor8[String, String, Option[String], Option[String], Option[SqsExpectedMessageDetails], Option[SnsExpectedMessageDetails], () => Unit, String] = Table(
    ("description", "input", "emailBody", "slackBody", "sqsMessage", "snsMessage", "stubContext", "slackUrl"),
    ("a GovUk key rotation event on intg",
      rotationEventInputText(intgApiKeyRotationEvent), None, expectedSlackMessageForApiKey(intgApiKeyRotationEvent), None, None, () => (), "/webhook"),
    ("a GovUk key rotation event on staging",
      rotationEventInputText(stagingApiKeyRotationEvent), None, expectedSlackMessageForApiKey(stagingApiKeyRotationEvent), None, None, () => (), "/webhook"),
    ("a GovUk key rotation event on prod",
      rotationEventInputText(prodApiKeyRotationEvent), None, expectedSlackMessageForApiKey(prodApiKeyRotationEvent), None, None, () => (), "/webhook"),
    ("a GitHub access token rotation event",
      rotationEventInputText(mgmtApiKeyRotationEvent), None, expectedSlackMessageForGitHubAccessToken(mgmtApiKeyRotationEvent), None, None, () => (), "/webhook"),
    ("Unknown event",
      rotationEventInputText(unknownEvent), None, expectedSlackMessageForUnknownEven, None, None, () => (), "/webhook")
  )

  private lazy val intgApiKeyRotationEvent = SNSNotifyEvent(Detail("/intg/keycloak/govuk_notify/api_key", "No change notification message"))
  private lazy val stagingApiKeyRotationEvent = SNSNotifyEvent(Detail("/staging/keycloak/govuk_notify/api_key", "No change notification message"))
  private lazy val prodApiKeyRotationEvent = SNSNotifyEvent(Detail("/prod/keycloak/govuk_notify/api_key", "No change notification message"))

  private lazy val mgmtApiKeyRotationEvent = SNSNotifyEvent(Detail("/mgmt/github/access_token", "No change notification message"))
  private lazy val unknownEvent = SNSNotifyEvent(Detail("/intg/parameter/unknown", "No change notification message"))

  private def rotationEventInputText(keyRotationEvent: SNSNotifyEvent): String = {
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

  private def expectedSlackMessageForApiKey(rotationEvent: SNSNotifyEvent): Option[String] = {
    val ssmParameter: String = rotationEvent.detail.`parameter-name`
    val reason: String = rotationEvent.detail.`action-reason`

    s"""{
       |  "blocks": [
       |    {
       |      "type": "section",
       |      "text": {
       |        "type": "mrkdwn",
       |        "text": ":warning: *Rotate GOV.UK Notify API Key*\\n*$ssmParameter*: $reason\\n\\nSee here for instructions to rotate GOV.UK Notify API Keys: https://github.com/nationalarchives/tdr-dev-documentation-internal/blob/main/manual/govuk-notify.md#rotating-api-key"
       |      }
       |    }
       |  ]
       |}
       |""".stripMargin.some
  }

  private def expectedSlackMessageForGitHubAccessToken(rotationEvent: SNSNotifyEvent): Option[String] = {
    val ssmParameter: String = rotationEvent.detail.`parameter-name`
    val reason: String = rotationEvent.detail.`action-reason`

    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":warning: *Rotate GitHub access token*\\n*$ssmParameter*: $reason\\n\\nSee here for instructions to rotate GitHub access token: https://github.com/nationalarchives/tdr-dev-documentation-internal/blob/main/manual/notify-github-access-token.md#rotate-github-personal-access-token"
       |    }
       |  } ]
       |}
       |""".stripMargin.some
  }

  private def expectedSlackMessageForUnknownEven: Option[String] = {
    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":error: *Unknown notify event*\\n*/intg/parameter/unknown*: No change notification message"
       |    }
       |  } ]
       |}""".stripMargin.some
  }
}
