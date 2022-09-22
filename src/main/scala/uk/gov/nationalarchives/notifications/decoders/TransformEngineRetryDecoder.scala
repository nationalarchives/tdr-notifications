package uk.gov.nationalarchives.notifications.decoders

import uk.gov.nationalarchives.notifications.messages.EventMessages.{ExportMessage, TransformEngineSqsMessage}

object TransformEngineRetryDecoder {
  case class TransformEngineRetryEvent(`consignment-reference`: String,
                                       `consignment-type`: String,
                                       `number-of-retries`: Int
                                      ) extends IncomingEvent with TransformEngineSqsMessage with ExportMessage {
    val consignmentReference: String = `consignment-reference`
    val consignmentType: String = `consignment-type`
    val numberOfRetries: Int = `number-of-retries`
  }

  //This will be the case class for the new v2 retry message model
  case class TransformEngineV2RetryEvent() extends IncomingEvent
}
