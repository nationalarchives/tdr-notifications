package uk.gov.nationalarchives.notifications

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, postRequestedFor, urlEqualTo}
import org.scalatest.prop.TableFor5
import uk.gov.nationalarchives.notifications.decoders.KeycloakEventDecoder.KeycloakEvent

class KeycloakEventIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: TableFor5[String, String, Option[String], Option[String], () => ()] = Table(
    ("description", "input", "emailBody", "slackBody", "stubContext"),
    ("a keycloak event message", scanEventInputText(keycloakEvent), None, Some(expectedKeycloakEventSlackMessage), () => ())
  )
  private lazy val keycloakEvent = KeycloakEvent("tdrEnv", "Some keycloak event message")

  def scanEventInputText(keycloakEvent: KeycloakEvent): String = {
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"tdrEnv\\":\\"${keycloakEvent.tdrEnv}\\",\\"message\\":\\"${keycloakEvent.message}\\"}"
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
       |      "text" : ":warning: Keycloak Event tdrEnv: Some keycloak event message"
       |    }
       |  } ]
       |}""".stripMargin
  }

  "the process method" should "not send a slack message if the environment is intg" in {
    val keycloakEvent = KeycloakEvent("intg", "Some keycloak event message")
    val input = scanEventInputText(keycloakEvent)
    val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
    new Lambda().process(stream, null)
    wiremockSlackServer.verify(0,
      postRequestedFor(urlEqualTo("/webhook"))
    )
  }
}
