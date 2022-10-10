package uk.gov.nationalarchives.notifications.decoders

//Incoming messages what TRE send to us
object TransformEngineRetryDecoderV2 {

  //This will be the case class for the new v2 retry message model
  case class UUIDs(producerName: Option[String], uuid: Option[String])

  case class Producer(environment: String, name: String, process: String, `event-name`: String, `type`: String)

  case class Resource(`resource-type`: String, `access-type`: String, value: String)

  case class ResourceValidation(`resource-type`: String, `access-type`: String, `validation-method`: String, value: String)

  case class NewBagit(resource: Resource, resourceValidation: ResourceValidation, reference: String)

  case class BagitValidationError(reference: String, errors: Option[List[String]])

  case class Parameters(`new-bagit`: Option[NewBagit] = None, `bagit-validation-error`: Option[BagitValidationError] = None)

/*  case class SnsExportMessageBody(`version`: String, `timestamp`: Long, UUIDs: List[UUIDs],
                                  producer: Producer,
                                  parameters: Parameters)*/

  case class TransformEngineV2RetryEvent(`version`: String, `timestamp`: Long, UUIDs: List[Map[String, String]],
                                         producer: Producer,
                                         parameters: Parameters) extends IncomingEvent
}
