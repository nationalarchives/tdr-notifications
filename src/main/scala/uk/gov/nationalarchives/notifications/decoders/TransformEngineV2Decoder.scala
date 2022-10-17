package uk.gov.nationalarchives.notifications.decoders

import io.circe.{Encoder, Json}

import java.util.UUID

object TransformEngineV2Decoder {
  val treVersion = "1.0.0"

  trait TransformEngineV2Event

  trait Parameters

  trait UUIDs

  case class ErrorParameters(`bagit-validation-error`: BagitValidationError) extends Parameters

  case class NewBagitParameters(`new-bagit`: NewBagit) extends Parameters

  case class Producer(environment: String,
                      name: String = "TDR",
                      process: String = "tdr-export-process",
                      `event-name`: String = "new-bagit",
                      `type`: String)

  case class Resource(`resource-type`: String = "Object", `access-type`: String = "url", value: String)

  case class ResourceValidation(`resource-type`: String = "Object",
                                `access-type`: String = "url",
                                `validation-method`: String = "SHA256",
                                value: String)

  case class NewBagit(resource: Resource, resourceValidation: ResourceValidation, reference: String)

  case class BagitValidationError(reference: String, errors: Option[List[String]])

  case class TdrUUID(`TDR-UUID`: UUID) extends UUIDs

  case class TreUUID(`TRE-UUID`: UUID) extends UUIDs

  case class TransformEngineV2RetryEvent(`version`: String, `timestamp`: Long, UUIDs: List[UUIDs],
                                         producer: Producer,
                                         parameters: ErrorParameters) extends IncomingEvent with TransformEngineV2Event

  case class TransferEngineV2NewBagitEvent(`version`: String, `timestamp`: Long, UUIDs: List[UUIDs],
                                           producer: Producer,
                                           parameters: NewBagitParameters) extends IncomingEvent with TransformEngineV2Event


  implicit val encodeUUIDs: Encoder[UUIDs] = {
    case TdrUUID(uuid) => Json.obj(("TDR-UUID", Json.fromString(uuid.toString)))
    case TreUUID(uuid) => Json.obj(("TRE-UUID", Json.fromString(uuid.toString)))
  }
}
