package uk.gov.nationalarchives.notifications

import io.circe.Printer

import org.scalatest.prop.TableFor8
import io.circe.syntax._
import io.circe.generic.auto._
import uk.gov.nationalarchives.notifications.decoders.StepFunctionErrorDecoder.StepFunctionError

import java.util.UUID

class StepFunctionErrorIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "a step function error on intg",
      input = stepFunctionErrorInput(stepFunctionError("intg")),
      expectedOutput = ExpectedOutput()
    ),
    Event(
      description = "a step function error on staging",
      input = stepFunctionErrorInput(stepFunctionError("staging")),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(slackMessage(stepFunctionError("staging")).get, "/webhook"))
      )
    ),
    Event(
      description = "a step function error on prod",
      input = stepFunctionErrorInput(stepFunctionError("prod")),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(slackMessage(stepFunctionError("prod")).get, "/webhook"))
      )
    )
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
