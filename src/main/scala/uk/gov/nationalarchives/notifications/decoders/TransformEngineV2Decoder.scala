package uk.gov.nationalarchives.notifications.decoders

import io.circe.{Encoder, Json}

import java.util.UUID

object TransformEngineV2Decoder {
  val treVersion = "1.0.0"
  val resourceType = "Object"
  val accessType = "url"

  trait Parameters

  trait UUIDs

  trait TransformEngineV2Event {
    def `version`: String

    def `timestamp`: Long

    def UUIDs: List[UUIDs]

    def producer: Producer

    def parameters: Parameters

    def retryEvent: Boolean = {
      producer.`event-name` == "bagit-validation-error"
    }
  }

  trait ResourceDetails {
    def `resource-type`: String

    def `access-type`: String

    def value: String
  }

  case class ErrorParameters(`bagit-validation-error`: BagitValidationError) extends Parameters

  case class BagitAvailableParameters(`bagit-available`: BagitAvailable) extends Parameters

  case class Producer(environment: String,
                      name: String = "TDR",
                      process: String = "tdr-export-process",
                      `event-name`: String = "bagit-available",
                      `type`: String)

  case class Resource(`resource-type`: String = resourceType, `access-type`: String = accessType, value: String) extends ResourceDetails

  case class ResourceValidation(`resource-type`: String = resourceType,
                                `access-type`: String = accessType,
                                `validation-method`: String = "SHA256",
                                value: String) extends ResourceDetails

  case class BagitAvailable(resource: Resource, `resource-validation`: ResourceValidation, reference: String)

  case class BagitValidationError(reference: String, errors: Option[List[String]])

  case class TdrUUID(`TDR-UUID`: UUID) extends UUIDs

  case class TreUUID(`TRE-UUID`: UUID) extends UUIDs

  case class TransformEngineV2OutEvent(`version`: String = treVersion, `timestamp`: Long, UUIDs: List[UUIDs],
                                       producer: Producer,
                                       parameters: ErrorParameters) extends IncomingEvent with TransformEngineV2Event

  case class TransferEngineV2InEvent(`version`: String = treVersion, `timestamp`: Long, UUIDs: List[UUIDs],
                                     producer: Producer,
                                     parameters: BagitAvailableParameters) extends TransformEngineV2Event

  case class Message(Message: TransformEngineV2Event)


  implicit val encodeUUIDs: Encoder[UUIDs] = {
    case TdrUUID(uuid) => Json.obj(("TDR-UUID", Json.fromString(uuid.toString)))
    case TreUUID(uuid) => Json.obj(("TRE-UUID", Json.fromString(uuid.toString)))
  }
}
