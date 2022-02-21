package uk.gov.nationalarchives.notifications.decoders

import java.util.UUID

object ExportStatusDecoder {
  case class ExportSuccessDetails(userId: UUID,
                                  consignmentReference: String,
                                  transferringBodyName: String,
                                  consignmentType: String,
                                  exportBucket: String)
  case class ExportStatusEvent(
                                consignmentId: UUID,
                                success: Boolean,
                                environment: String,
                                successDetails: Option[ExportSuccessDetails],
                                failureCause: Option[String]) extends IncomingEvent
}
