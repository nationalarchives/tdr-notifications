package uk.gov.nationalarchives.notifications

import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import uk.gov.nationalarchives.notifications.decoders.DraftMetadataStepFunctionErrorDecoder.DraftMetadataStepFunctionError

import java.util.UUID

class DraftMetadataStepFunctionErrorIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "a draft metadata step function error on intg",
      input = draftMetaDataStepFunctionErrorInput(draftMetadataStepFunctionError("intg")),
      expectedOutput = ExpectedOutput()
    ),
    Event(
      description = "a draft metadata step function error on staging",
      input = draftMetaDataStepFunctionErrorInput(draftMetadataStepFunctionError("staging")),
      expectedOutput = ExpectedOutput()
    ),
    Event(
      description = "a draft metadata step function error on prod",
      input = draftMetaDataStepFunctionErrorInput(draftMetadataStepFunctionError("prod")),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(slackMessage(draftMetadataStepFunctionError("prod")).get, "/webhook"))
      )
    )
  )


  def draftMetadataStepFunctionError(environment: String): DraftMetadataStepFunctionError = {
    val id = UUID.fromString("49d364ab-8bc3-4c53-90ca-d3f003179cb9")
    DraftMetadataStepFunctionError(id, "error", "cause", environment)
  }

  def draftMetaDataStepFunctionErrorInput(draftMetadataStepFunctionError: DraftMetadataStepFunctionError): String = {
    draftMetadataStepFunctionError.asJson.printWith(Printer.noSpaces)
  }

  def slackMessage(draftMetadataStepFunctionError: DraftMetadataStepFunctionError): Option[String] = Option {
    s"""{
       |  "blocks": [
       |    {
       |      "type": "section",
       |      "text": {
       |        "type": "mrkdwn",
       |        "text": ":warning: *DraftMetadata upload has failed for consignment*\\n*ConsignmentId* ${draftMetadataStepFunctionError.consignmentId}\\n*Environment* ${draftMetadataStepFunctionError.environment}\\n*Cause*: ${draftMetadataStepFunctionError.cause}\\n*Error*: ${draftMetadataStepFunctionError.metaDataError}"
       |      }
       |    }
       |  ]
       |}
       |""".stripMargin
  }
}
