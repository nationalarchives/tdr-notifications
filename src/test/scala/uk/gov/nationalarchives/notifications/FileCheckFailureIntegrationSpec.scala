package uk.gov.nationalarchives.notifications

import uk.gov.nationalarchives.notifications.decoders.FileCheckFailureDecoder.FileCheckFailureEvent
import uk.gov.nationalarchives.notifications.messages.EventMessages.fileCheckFailureEventMessages
import uk.gov.nationalarchives.notifications.messages.EventMessages.GovUKEmailDetails

import java.util.UUID

class FileCheckFailureIntegrationSpec extends LambdaIntegrationSpec {

  private lazy val consignmentId = "c2e7e539-0410-4dbf-b96e-1e3871d868ad"
  private lazy val userId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  private lazy val resolutionPath = "TNASupport"

  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "A file check failure event for standard on prod",
      input = fileCheckFailureInputString(
        consignmentType = "standard",
        consignmentReference = "TDR-2025-ABC",
        consignmentId = consignmentId,
        transferringBodyName = "SomeTransferringBody",
        userId = userId,
        environment = "prod",
        resolutionPath = resolutionPath
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("standard", "TDR-2025-ABC", consignmentId, "SomeTransferringBody", userId, "prod", resolutionPath, "tdr.nationalarchives.gov.uk"),
            webhookUrl = "/webhook-transfers"
          )
        ),
        govUKEmail = Some(
          GovUKEmailDetails(
            templateId = "TestFileCheckFailureTemplateId",
            userEmail = "tdr@nationalarchives.gov.uk",
            personalisation = Map(
              "environment" -> "prod",
              "consignmentType" -> "standard",
              "consignmentReference" -> "TDR-2025-ABC",
              "consignmentId" -> consignmentId,
              "transferringBodyName" -> "SomeTransferringBody",
              "userId" -> userId,
              "resolutionPath" -> resolutionPath
            ),
            reference = s"TDR-2025-ABC-$userId"
          )
        )
      )
    ),
    Event(
      description = "A file check failure event for judgment on prod",
      input = fileCheckFailureInputString(
        consignmentType = "judgment",
        consignmentReference = "TDR-2025-JDG",
        consignmentId = consignmentId,
        transferringBodyName = "SomeTransferringBody",
        userId = userId,
        environment = "prod",
        resolutionPath = resolutionPath
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("judgment", "TDR-2025-JDG", consignmentId, "SomeTransferringBody", userId, "prod", resolutionPath, "tdr.nationalarchives.gov.uk"),
            webhookUrl = "/webhook-transfers"
          )
        ),
        govUKEmail = Some(
          GovUKEmailDetails(
            templateId = "TestFileCheckFailureTemplateId",
            userEmail = "tdr@nationalarchives.gov.uk",
            personalisation = Map(
              "environment" -> "prod",
              "consignmentType" -> "judgment",
              "consignmentReference" -> "TDR-2025-JDG",
              "consignmentId" -> consignmentId,
              "transferringBodyName" -> "SomeTransferringBody",
              "userId" -> userId,
              "resolutionPath" -> resolutionPath
            ),
            reference = s"TDR-2025-JDG-$userId"
          )
        )
      )
    ),
    Event(
      description = "A file check failure event for standard on non-prod",
      input = fileCheckFailureInputString(
        consignmentType = "standard",
        consignmentReference = "TDR-2025-DEF",
        consignmentId = consignmentId,
        transferringBodyName = "SomeTransferringBody",
        userId = userId,
        environment = "intg",
        resolutionPath = resolutionPath
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("standard", "TDR-2025-DEF", consignmentId, "SomeTransferringBody", userId, "intg", resolutionPath, "tdr-integration.nationalarchives.gov.uk"),
            webhookUrl = "/webhook-releases"
          )
        ),
        govUKEmail = Some(
          GovUKEmailDetails(
            templateId = "TestFileCheckFailureTemplateId",
            userEmail = "tdr@nationalarchives.gov.uk",
            personalisation = Map(
              "environment" -> "intg",
              "consignmentType" -> "standard",
              "consignmentReference" -> "TDR-2025-DEF",
              "consignmentId" -> consignmentId,
              "transferringBodyName" -> "SomeTransferringBody",
              "userId" -> userId,
              "resolutionPath" -> resolutionPath
            ),
            reference = s"TDR-2025-DEF-$userId"
          )
        )
      )
    ),
    Event(
      description = "A file check failure event for standard with MOCK transferring body",
      input = fileCheckFailureInputString(
        consignmentType = "standard",
        consignmentReference = "TDR-2025-GHI",
        consignmentId = consignmentId,
        transferringBodyName = "Mock 1 Department",
        userId = userId,
        environment = "intg",
        resolutionPath = resolutionPath
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = None
      )
    ),
    Event(
      description = "A file check failure event for judgment on non-prod",
      input = fileCheckFailureInputString(
        consignmentType = "judgment",
        consignmentReference = "TDR-2025-JKL",
        consignmentId = consignmentId,
        transferringBodyName = "SomeTransferringBody",
        userId = userId,
        environment = "intg",
        resolutionPath = resolutionPath
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        slackMessage = Some(
          SlackMessage(
            body = slackMessage("judgment", "TDR-2025-JKL", consignmentId, "SomeTransferringBody", userId, "intg", resolutionPath, "tdr-integration.nationalarchives.gov.uk"),
            webhookUrl = "/webhook-releases"
          )
        ),
        govUKEmail = Some(
          GovUKEmailDetails(
            templateId = "TestFileCheckFailureTemplateId",
            userEmail = "tdr@nationalarchives.gov.uk",
            personalisation = Map(
              "environment" -> "intg",
              "consignmentType" -> "judgment",
              "consignmentReference" -> "TDR-2025-JKL",
              "consignmentId" -> consignmentId,
              "transferringBodyName" -> "SomeTransferringBody",
              "userId" -> userId,
              "resolutionPath" -> resolutionPath
            ),
            reference = s"TDR-2025-JKL-$userId"
          )
        )
      )
    ),
    Event(
      description = "A file check failure event for judgment with MOCK transferring body on non-prod",
      input = fileCheckFailureInputString(
        consignmentType = "judgment",
        consignmentReference = "TDR-2025-MNO",
        consignmentId = consignmentId,
        transferringBodyName = "Mock 1 Department",
        userId = userId,
        environment = "intg",
        resolutionPath = resolutionPath
      ),
      expectedOutput = ExpectedOutput(
        slackMessage = None
      )
    )
  )

  Seq(
    "integration" -> "tdr-integration.nationalarchives.gov.uk",
    "staging" -> "tdr-staging.nationalarchives.gov.uk"
  ).foreach { case (environment, hostname) =>
    "the file check failure slack message" should s"include the errors download link for $environment" in {
      val event = FileCheckFailureEvent(
        consignmentType = "standard",
        consignmentReference = "TDR-2025-ABC",
        consignmentId = UUID.fromString(consignmentId),
        transferringBodyName = "SomeTransferringBody",
        userId = UUID.fromString(userId),
        environment = environment,
        resolutionPath = resolutionPath
      )

      val message = fileCheckFailureEventMessages.slack(event, ())

      message.map(_.blocks.head.text.text) shouldBe Some(
        s":warning: *A user has experienced a File Check Failure*\n*Environment*: $environment\n*Consignment Type*: standard\n*Consignment Reference*: TDR-2025-ABC\n*Consignment ID*: $consignmentId\n*Transferring Body*: SomeTransferringBody\n*UserID*: $userId\n*Resolution Path*: $resolutionPath\n<https://$hostname/admin/file-checks-failures/$consignmentId/TDR-2025-ABC|View error report>"
      )
    }
  }

  private def slackMessage(consignmentType: String, consignmentReference: String, consignmentId: String, transferringBodyName: String, userId: String, environment: String, resolutionPath: String, hostname: String): String = {
    s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : ":warning: *A user has experienced a File Check Failure*\\n*Environment*: $environment\\n*Consignment Type*: $consignmentType\\n*Consignment Reference*: $consignmentReference\\n*Consignment ID*: $consignmentId\\n*Transferring Body*: $transferringBodyName\\n*UserID*: $userId\\n*Resolution Path*: $resolutionPath\\n<https://$hostname/admin/file-checks-failures/$consignmentId/$consignmentReference|View error report>"
       |    }
       |  } ]
       |}
       |""".stripMargin
  }

  private def fileCheckFailureInputString(consignmentType: String, consignmentReference: String, consignmentId: String, transferringBodyName: String, userId: String, environment: String, resolutionPath: String): String = {
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"consignmentType\\":\\"$consignmentType\\",\\"consignmentReference\\":\\"$consignmentReference\\",\\"consignmentId\\":\\"$consignmentId\\",\\"transferringBodyName\\":\\"$transferringBodyName\\",\\"userId\\":\\"$userId\\",\\"environment\\":\\"$environment\\",\\"resolutionPath\\":\\"$resolutionPath\\"}"
       |      }
       |    }
       |  ]}""".stripMargin
  }
}
