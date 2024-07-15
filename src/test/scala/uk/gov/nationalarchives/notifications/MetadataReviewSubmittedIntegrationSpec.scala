package uk.gov.nationalarchives.notifications

import uk.gov.nationalarchives.notifications.decoders.MetadataReviewSubmittedDecoder.MetadataReviewSubmittedEvent
import uk.gov.nationalarchives.notifications.messages.EventMessages.GovUKEmailDetails

class MetadataReviewSubmittedIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "A metadata review submitted event",
      input = metadataReviewSubmittedNotificationInputString(
        MetadataReviewSubmittedEvent(
          transferringBodyName = "SomeTransferringBody",
          consignmentReference = "SomeConsignmentReference",
          consignmentId = "SomeConsignmentId",
          userId = "SomeUserId",
          userEmail = "test@test.test",
          urlLink = "example.com",
        )
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        govUKEmail = Some(
          GovUKEmailDetails(
            reference = "SomeConsignmentReference",
            templateId = "TestTemplateId",
            userEmail = "tdr@nationalarchives.gov.uk",
            personalisation = Map(
              "urlLink" -> "example.com",
              "userEmail" -> "test@test.test",
              "consignmentReference" -> "SomeConsignmentReference",
              "userId" -> "SomeUserId",
              "consignmentId" -> "SomeConsignmentId",
              "transferringBodyName" -> "SomeTransferringBody",
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
       |       "Message": "{\\"urlLink\\" : \\"$urlLink\\",\\"transferringBodyName\\":\\"$transferringBodyName\\",\\"consignmentReference\\":\\"$consignmentReference\\",\\"consignmentId\\" : \\"$consignmentId\\",\\"userId\\" : \\"$userId\\",\\"userEmail\\" : \\"$userEmail\\"}"
       |      }
       |    }
       |  ]}""".stripMargin
  }
}
