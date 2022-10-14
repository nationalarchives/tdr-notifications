package uk.gov.nationalarchives.notifications.decoders

object TransformEngineV2Decoder {

  trait TransformEngineV2

  trait Parameters

  case class ErrorParameters(`bagit-validation-error`: BagitValidationError) extends Parameters

  case class NewBagitParameters(`new-bagit`: NewBagit) extends Parameters

  case class Producer(environment: String, name: String, process: String, `event-name`: String, `type`: String)

  case class Resource(`resource-type`: String, `access-type`: String, value: String)

  case class ResourceValidation(`resource-type`: String, `access-type`: String, `validation-method`: String, value: String)

  case class NewBagit(resource: Resource, resourceValidation: ResourceValidation, reference: String)

  case class BagitValidationError(reference: String, errors: Option[List[String]])

  case class TransformEngineV2RetryEvent(`version`: String, `timestamp`: Long, UUIDs: List[Map[String, String]],
                                         producer: Producer,
                                         parameters: ErrorParameters) extends IncomingEvent with TransformEngineV2

  case class TransferEngineV2Event(`version`: String, `timestamp`: Long, UUIDs: List[Map[String, String]],
                                   producer: Producer,
                                   parameters:NewBagitParameters) extends IncomingEvent with TransformEngineV2
}
