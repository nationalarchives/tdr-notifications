package uk.gov.nationalarchives.notifications.decoders

import io.circe.{Encoder, Json}
import uk.gov.nationalarchives.common.messages.Properties
import uk.gov.nationalarchives.da.messages.bag.available

import java.util.UUID

object TransformEngineV2Decoder {
  val treVersion = "1.0.0"
  val resourceType = "Object"
  val accessType = "url"

  trait UUIDs

  case class Producer(environment: String,
                      name: String = "TDR",
                      process: String = "tdr-export-process",
                      `event-name`: String = "bagit-available",
                      `type`: String)

  case class TdrUUID(`TDR-UUID`: UUID) extends UUIDs

  case class TreUUID(`TRE-UUID`: UUID) extends UUIDs

  case class TransferEngineV2InEvent(properties : Properties, parameters : available.Parameters)

  implicit val encodeUUIDs: Encoder[UUIDs] = {
    case TdrUUID(uuid) => Json.obj(("TDR-UUID", Json.fromString(uuid.toString)))
    case TreUUID(uuid) => Json.obj(("TRE-UUID", Json.fromString(uuid.toString)))
  }

  implicit val encodeProperties: Encoder[Properties] =
    Encoder.forProduct6[Properties, String, String, String, String, String, Option[String]]("messageType", "timestamp", "function", "producer", "executionId", "parentExecutionId") {
      case Properties(messageType, timestamp, function, producer, executionId, parentExecutionId) => (messageType, timestamp, function, producer.toString, executionId, parentExecutionId)
    }

  implicit val encodeParameters: Encoder[available.Parameters] = {
    Encoder.forProduct6[available.Parameters, String, String, String, String, String, String]("reference", "consignmentType", "originator", "s3Bucket", "s3Key", "s3BagSha256Key") {
      case available.Parameters(reference, consignmentType, originator, s3Bucket, s3Key, s3BagSha256Key) => (reference, consignmentType.toString, originator.getOrElse(""), s3Bucket, s3Key, s3BagSha256Key)
    }
  }
}
