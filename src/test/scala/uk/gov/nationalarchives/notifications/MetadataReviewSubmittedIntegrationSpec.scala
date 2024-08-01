package uk.gov.nationalarchives.notifications

import uk.gov.nationalarchives.notifications.decoders.MetadataReviewSubmittedDecoder.MetadataReviewSubmittedEvent
import uk.gov.nationalarchives.notifications.messages.EventMessages.GovUKEmailDetails

class MetadataReviewSubmittedIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "An approved metadata review submitted event",
      input = metadataReviewSubmittedNotificationInputString(
        MetadataReviewSubmittedEvent(
          consignmentReference = "SomeConsignmentReference",
          urlLink = "example.com",
          userEmail = "email@mail.com",
          status = "Completed"
        )
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        govUKEmail = Some(
          GovUKEmailDetails(
            reference = "SomeConsignmentReference",
            templateId = "TestApprovedTemplateId",
            userEmail = "email@mail.com",
            personalisation = Map(
              "urlLink" -> "example.com",
              "consignmentReference" -> "SomeConsignmentReference",
            )
          )
        )
      )
    ),
    Event(
      description = "A rejected metadata review submitted event",
      input = metadataReviewSubmittedNotificationInputString(
        MetadataReviewSubmittedEvent(
          consignmentReference = "SomeConsignmentReference",
          urlLink = "example.com",
          userEmail = "email@mail.com",
          status = "CompletedWithIssues"
        )
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        govUKEmail = Some(
          GovUKEmailDetails(
            reference = "SomeConsignmentReference",
            templateId = "TestRejectedTemplateId",
            userEmail = "email@mail.com",
            personalisation = Map(
              "urlLink" -> "example.com",
              "consignmentReference" -> "SomeConsignmentReference",
            )
          )
        )
      )
    )
  )

  def metadataReviewSubmittedNotificationInputString(metadataReviewSubmittedEvent: MetadataReviewSubmittedEvent): String = {
    import metadataReviewSubmittedEvent._
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"consignmentReference\\":\\"$consignmentReference\\",\\"urlLink\\" : \\"$urlLink\\",\\"userEmail\\" : \\"$userEmail\\",\\"status\\" : \\"$status\\"}"
       |      }
       |    }
       |  ]}""".stripMargin
  }
}
