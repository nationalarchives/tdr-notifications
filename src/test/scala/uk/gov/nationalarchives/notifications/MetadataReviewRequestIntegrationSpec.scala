package uk.gov.nationalarchives.notifications

import uk.gov.nationalarchives.notifications.decoders.MetadataReviewRequestDecoder.MetadataReviewRequestEvent
import uk.gov.nationalarchives.notifications.messages.EventMessages.GovUKEmailDetails

class MetadataReviewRequestIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "A transfer complete event",
      input = metadataReviewRequestNotificationInputString(
        MetadataReviewRequestEvent(
          transferringBodyName = "SomeTransferringBody",
          consignmentReference = "SomeConsignmentReference",
          consignmentId = "SomeConsignmentId",
          userId = "SomeUserId",
          userEmail = "test@test.test"
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
              "userEmail" -> "test@test.test",
              "userId" -> "SomeUserId",
              "transferringBodyName" -> "SomeTransferringBody",
              "consignmentId" -> "SomeConsignmentId",
              "consignmentReference" -> "SomeConsignmentReference",
            )
          )
        )
      )
    )
  )
  
  def metadataReviewRequestNotificationInputString(metadataReviewRequestEvent: MetadataReviewRequestEvent): String = {
    import metadataReviewRequestEvent._
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"transferringBodyName\\":\\"$transferringBodyName\\",\\"consignmentReference\\":\\"$consignmentReference\\",\\"consignmentId\\" : \\"$consignmentId\\",\\"userId\\" : \\"$userId\\",\\"userEmail\\" : \\"$userEmail\\"}"
       |      }
       |    }
       |  ]}""".stripMargin
  }
}
