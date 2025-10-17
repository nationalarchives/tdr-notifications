package uk.gov.nationalarchives.notifications

import uk.gov.nationalarchives.notifications.decoders.ParameterStoreExpiryEventDecoder.{Detail, ParameterStoreExpiryEvent}

class ParameterStoreExpiryEventSpec extends LambdaIntegrationSpec {

  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "a GovUk Notify key rotation event on intg",
      input = rotationEventInputText(intgApiKeyRotationEvent),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(expectedSlackMessageForApiKey(intgApiKeyRotationEvent), "/webhook-url"))
      )
    ),
    Event(
      description = "a GovUk Notify key rotation event on staging",
      input = rotationEventInputText(stagingApiKeyRotationEvent),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(expectedSlackMessageForApiKey(stagingApiKeyRotationEvent), "/webhook-url"))
      )
    ),
    Event(
      description = "a GovUk Notify key rotation event on prod",
      input = rotationEventInputText(prodApiKeyRotationEvent),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(expectedSlackMessageForApiKey(prodApiKeyRotationEvent), "/webhook-url"))
      )
    ),
    Event(
      description = "a GitHub access token rotation event",
      input = rotationEventInputText(mgmtApiKeyRotationEvent),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(expectedSlackMessageForGitHubAccessToken(mgmtApiKeyRotationEvent), "/webhook-url"))
      )
    ),
    Event(
      description = "a NPM token rotation event",
      input = rotationEventInputText(mgmtNpmTokenRotationEvent),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(expectedSlackMessageForNpmToken(mgmtNpmTokenRotationEvent), "/webhook-url"))
      )
    ),
    Event(
      description = "Unknown event",
      input = rotationEventInputText(unknownEvent),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(expectedSlackMessageForUnknownEvent, "/webhook-url"))
      )
    )
  )
  
  private lazy val intgApiKeyRotationEvent = ParameterStoreExpiryEvent(Detail("/intg/keycloak/govuk_notify/api_key", "No change notification message"))
  private lazy val stagingApiKeyRotationEvent = ParameterStoreExpiryEvent(Detail("/staging/keycloak/govuk_notify/api_key", "No change notification message"))
  private lazy val prodApiKeyRotationEvent = ParameterStoreExpiryEvent(Detail("/prod/keycloak/govuk_notify/api_key", "No change notification message"))

  private lazy val mgmtApiKeyRotationEvent = ParameterStoreExpiryEvent(Detail("/github_enterprise/access_token", "No change notification message"))
  private lazy val mgmtNpmTokenRotationEvent = ParameterStoreExpiryEvent(Detail("/npm_granular_token", "No change notification message"))
  private lazy val unknownEvent = ParameterStoreExpiryEvent(Detail("/intg/parameter/unknown", "No change notification message"))

  private def rotationEventInputText(keyRotationEvent: ParameterStoreExpiryEvent): String = {
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

  private def expectedSlackMessageForApiKey(rotationEvent: ParameterStoreExpiryEvent): String = {
    val ssmParameter: String = rotationEvent.detail.`parameter-name`
    val reason: String = rotationEvent.detail.`action-reason`

    s"""{
       |  "blocks": [
       |    {
       |      "type": "section",
       |      "text": {
       |        "type": "mrkdwn",
       |        "text": ":warning: *Rotate SSM Parameter Value*\\n*$ssmParameter*: $reason\\n\\nSee here for instructions to rotate GOV.UK Notify API Keys: https://github.com/nationalarchives/tdr-dev-documentation-internal/blob/main/manual/govuk-notify.md#rotating-api-key"
       |      }
       |    }
       |  ]
       |}
       |""".stripMargin
  }

  private def expectedSlackMessageForGitHubAccessToken(rotationEvent: ParameterStoreExpiryEvent): String = {
    val ssmParameter: String = rotationEvent.detail.`parameter-name`
    val reason: String = rotationEvent.detail.`action-reason`

    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":warning: *Rotate SSM Parameter Value*\\n*$ssmParameter*: $reason\\n\\nSee here for instructions to rotate GitHub access token: https://github.com/nationalarchives/tdr-dev-documentation-internal/blob/main/manual/rotate-tokens.md"
       |    }
       |  } ]
       |}
       |""".stripMargin
  }

  private def expectedSlackMessageForNpmToken(rotationEvent: ParameterStoreExpiryEvent): String = {
    val ssmParameter: String = rotationEvent.detail.`parameter-name`
    val reason: String = rotationEvent.detail.`action-reason`

    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":warning: *Rotate SSM Parameter Value*\\n*$ssmParameter*: $reason\\n\\nSee here for instructions to rotate NPM token: https://github.com/nationalarchives/tdr-dev-documentation-internal/blob/main/manual/rotate-tokens.md"
       |    }
       |  } ]
       |}
       |""".stripMargin
  }

  private def expectedSlackMessageForUnknownEvent: String = {
    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":error: *Unknown notify event*\\n*/intg/parameter/unknown*: No change notification message"
       |    }
       |  } ]
       |}""".stripMargin
  }
}
