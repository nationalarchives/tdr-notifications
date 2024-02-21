package uk.gov.nationalarchives.notifications.decoders

import java.util.UUID

import uk.gov.nationalarchives.notifications.messages.EventMessages.ExportMessage

object ExportStatusDecoder {
  case class ExportSuccessDetails(userId: UUID,
                                  consignmentReference: String,
                                  transferringBodyName: String,
                                  consignmentType: String,
                                  exportBucket: String) extends ExportMessage
  case class ExportStatusEvent(
                                consignmentId: UUID,
                                success: Boolean,
                                environment: String,
                                successDetails: Option[ExportSuccessDetails],
                                failureCause: Option[String]) extends IncomingEvent {
//    def mockEvent: Boolean = {
//      successDetails.exists(_.transferringBodyName.toUpperCase.contains("MOCK"))
//    }
  }
}
