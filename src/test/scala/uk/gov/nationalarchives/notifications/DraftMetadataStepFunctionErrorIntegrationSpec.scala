package uk.gov.nationalarchives.notifications

import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import uk.gov.nationalarchives.notifications.decoders.DraftMetadataStepFunctionErrorDecoder.DraftMetadataStepFunctionError
import uk.gov.nationalarchives.notifications.decoders.IncomingEvent.{SNS, SnsRecord}

import java.util.UUID

class DraftMetadataStepFunctionErrorIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "a draft metadata step function error on intg",
      input = draftMetaDataStepFunctionErrorSnsInput(draftMetadataStepFunctionError("intg")),
      expectedOutput = ExpectedOutput()
    ),
    Event(
      description = "a draft metadata step function error on staging",
      input = draftMetaDataStepFunctionErrorSnsInput(draftMetadataStepFunctionError("staging")),
      expectedOutput = ExpectedOutput()
    ),
    Event(
      description = "a draft metadata step function error on prod",
      input = draftMetaDataStepFunctionErrorSnsInput(draftMetadataStepFunctionError("prod")),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(slackMessage(draftMetadataStepFunctionError("prod")).get, "/webhook-tdr"))
      )
    )
  )


  def draftMetadataStepFunctionError(environment: String): DraftMetadataStepFunctionError = {
    val id = UUID.fromString("49d364ab-8bc3-4c53-90ca-d3f003179cb9")
    DraftMetadataStepFunctionError(id, "error", "cause", environment)
  }

  def draftMetaDataStepFunctionErrorSnsInput(draftMetadataStepFunctionError: DraftMetadataStepFunctionError): String = {
    case class Records(Records:List[SnsRecord])
    val snsEvent = Records(List(SnsRecord(SNS(draftMetadataStepFunctionError.asJson.toString()))))
    snsEvent.asJson.printWith(Printer.noSpaces)
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
