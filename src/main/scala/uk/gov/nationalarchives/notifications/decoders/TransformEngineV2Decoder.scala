package uk.gov.nationalarchives.notifications.decoders

object TransformEngineV2Decoder {

  trait TransformEngineV2Event

  trait Parameters

  case class UUIDs()

  case class ErrorParameters(`bagit-validation-error`: BagitValidationError) extends Parameters

  case class NewBagitParameters(`new-bagit`: NewBagit) extends Parameters

  case class Producer(environment: String,
                      name: String = "TDR",
                      process: String = "tdr-export-process",
                      `event-name`: String = "new-bagit",
                      `type`: String)

  case class Resource(`resource-type`: String, `access-type`: String, value: String)

  case class ResourceValidation(`resource-type`: String, `access-type`: String, `validation-method`: String, value: String)

  case class NewBagit(resource: Resource, resourceValidation: ResourceValidation, reference: String)

  case class BagitValidationError(reference: String, errors: Option[List[String]])

  case class TransformEngineV2RetryEvent(`version`: String, `timestamp`: Long, UUIDs: List[Map[String, String]],
                                         producer: Producer,
                                         parameters: ErrorParameters) extends IncomingEvent with TransformEngineV2Event

  case class TransferEngineV2NewBagitEvent(`version`: String, `timestamp`: Long, UUIDs: List[Map[String, String]],
                                           producer: Producer,
                                           parameters: NewBagitParameters) extends IncomingEvent with TransformEngineV2Event
}
