package uk.gov.nationalarchives.notifications

import java.util.UUID

import org.scalatest.prop.TableFor5
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportStatusEvent
import uk.gov.nationalarchives.notifications.decoders.KeycloakEventDecoder.KeycloakEvent

class KeycloakEventIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: TableFor5[String, String, Option[String], Option[String], () => ()] = Table(
    ("description", "input", "emailBody", "slackBody", "stubContext"),
    ("a keycloak event message", scanEventInputText(keycloakEvent), None, Some(expectedKeycloakEventSlackMessage), () => ())
  )
  private lazy val keycloakEvent = KeycloakEvent("Some keycloak event message")

  def scanEventInputText(keycloakEvent: KeycloakEvent): String = {
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "${keycloakEvent.message}"
       |      }
       |    }
       |  ]}""".stripMargin
  }

  private lazy val expectedKeycloakEventSlackMessage: String = {
    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":warning: Keycloak Event: Some keycloak event message"
       |    }
       |  } ]
       |}""".stripMargin
  }
}
