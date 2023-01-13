package uk.gov.nationalarchives.notifications

import io.circe.Printer

import org.scalatest.prop.TableFor8
import io.circe.syntax._
import io.circe.generic.auto._
import uk.gov.nationalarchives.notifications.decoders.StepFunctionErrorDecoder.StepFunctionError

import java.util.UUID

class StepFunctionErrorIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: TableFor8[String, String, Option[String], Option[String], Option[SqsExpectedMessageDetails], Option[SnsExpectedMessageDetails], () => Unit, String] = Table(
    ("description", "input", "emailBody", "slackBody", "sqsMessage", "snsMessage", "stubContext", "slackUrl"),
    ("a step function error on intg",
      stepFunctionErrorInput(stepFunctionError("intg")), None, None, None, None, () => (), "/webhook"),
    ("a step function error on staging",
      stepFunctionErrorInput(stepFunctionError("staging")), None, slackMessage(stepFunctionError("staging")), None, None, () => (), "/webhook"),
    ("a step function error on prod",
      stepFunctionErrorInput(stepFunctionError("prod")), None, slackMessage(stepFunctionError("prod")), None, None, () => (), "/webhook"),
  )

  def stepFunctionError(environment: String): StepFunctionError = {
    val id = UUID.fromString("49d364ab-8bc3-4c53-90ca-d3f003179cb9")
    StepFunctionError(id ,"error", "cause", environment)
  }

  def stepFunctionErrorInput(stepFunctionError: StepFunctionError): String = {
    stepFunctionError.asJson.printWith(Printer.noSpaces)
  }

  def  slackMessage(stepFunctionError: StepFunctionError): Option[String] = Option {
    s"""{
       |  "blocks": [
       |    {
       |      "type": "section",
       |      "text": {
       |        "type": "mrkdwn",
       |        "text": ":warning: *Backend checks failure for consignment*\\n*ConsignmentId* ${stepFunctionError.consignmentId}\\n*Environment* ${stepFunctionError.environment}\\n*Cause*: ${stepFunctionError.cause}\\n*Error*: ${stepFunctionError.error}"
       |      }
       |    }
       |  ]
       |}
       |""".stripMargin
  }
}
