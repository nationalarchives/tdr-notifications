package uk.gov.nationalarchives.notifications

import uk.gov.nationalarchives.notifications.decoders.TransferCompleteEventDecoder.TransferCompleteEvent
import uk.gov.nationalarchives.notifications.messages.EventMessages.GovUKEmailDetails

class TransferCompleteIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "A transfer complete DTA event",
      input = transferCompleteNotificationInputString(
        TransferCompleteEvent(
          transferringBodyName = "SomeTransferringBody",
          consignmentReference = "SomeConsignmentReference",
          consignmentId = "SomeConsignmentId",
          seriesName = "SomeSeriesName",
          userId = "SomeUserId",
          userEmail = "test@test.test"
        )
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        govUKEmail = Some(
          GovUKEmailDetails(
            reference = "SomeConsignmentReference-SomeUserId",
            templateId = "TestTransferDTATemplateId",
            userEmail = "tdr@nationalarchives.gov.uk",
            personalisation = Map(
              "userEmail" -> "test@test.test",
              "userId" -> "SomeUserId",
              "transferringBodyName" -> "SomeTransferringBody",
              "consignmentId" -> "SomeConsignmentId",
              "consignmentReference" -> "SomeConsignmentReference",
              "seriesName" -> "SomeSeriesName"
            )
          )
        )
      )
    ),
    Event(
      description = "A transfer complete TB event",
      input = transferCompleteNotificationInputString(
        TransferCompleteEvent(
          transferringBodyName = "SomeTransferringBody",
          consignmentReference = "SomeConsignmentReference",
          consignmentId = "SomeConsignmentId",
          seriesName = "SomeSeriesName",
          userId = "SomeUserId",
          userEmail = "test@test.test"
        )
      ),
      stubContext = stubDummyGovUkNotifyEmailResponse,
      expectedOutput = ExpectedOutput(
        govUKEmail = Some(
          GovUKEmailDetails(
            reference = "SomeConsignmentReference",
            templateId = "TestTransferTBTemplateId",
            userEmail = "test@test.test",
            personalisation = Map(
              "consignmentReference" -> "SomeConsignmentReference",
            )
          )
        )
      )
    )
  )
  
  def transferCompleteNotificationInputString(transferCompleteEvent: TransferCompleteEvent): String = {
    import transferCompleteEvent._
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"transferringBodyName\\":\\"$transferringBodyName\\",\\"consignmentReference\\":\\"$consignmentReference\\",\\"consignmentId\\" : \\"$consignmentId\\",\\"seriesName\\" : \\"$seriesName\\",\\"userId\\" : \\"$userId\\",\\"userEmail\\" : \\"$userEmail\\"}"
       |      }
       |    }
       |  ]}""".stripMargin
  }
}
