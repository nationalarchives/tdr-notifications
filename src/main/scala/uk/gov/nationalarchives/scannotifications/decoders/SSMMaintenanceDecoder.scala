package uk.gov.nationalarchives.scannotifications.decoders

import io.circe.{Decoder, HCursor}

object SSMMaintenanceDecoder {

  case class SSMMaintenanceEvent(success: Boolean) extends IncomingEvent

  val decodeMaintenanceEvent: Decoder[IncomingEvent] = (c: HCursor) => for {
    status <- c.downField("detail").downField("status").as[String]
  } yield SSMMaintenanceEvent(status != "FAILED")

}
