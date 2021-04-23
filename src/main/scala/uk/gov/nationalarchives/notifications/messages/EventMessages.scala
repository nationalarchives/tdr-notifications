package uk.gov.nationalarchives.notifications.messages

import java.net.URI
import cats.effect.IO
import cats.implicits._
import com.typesafe.config.ConfigFactory
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scalatags.Text.all._
import software.amazon.awssdk.services.ecr.model.FindingSeverity
import uk.gov.nationalarchives.aws.utils.SESUtils.Email
import uk.gov.nationalarchives.aws.utils.{Clients, ECRUtils, SESUtils}
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportStatusEvent
import uk.gov.nationalarchives.notifications.decoders.SSMMaintenanceDecoder.SSMMaintenanceEvent
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.{ScanDetail, ScanEvent}
import uk.gov.nationalarchives.notifications.messages.Messages.eventConfig

import scala.jdk.CollectionConverters.CollectionHasAsScala

object EventMessages {

  implicit def logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  case class SlackText(`type`: String, text: String)

  case class SlackBlock(`type`: String, text: SlackText)

  case class SlackMessage(blocks: List[SlackBlock])

  implicit val scanEventMessages: Messages[ScanEvent, ImageScanReport] = new Messages[ScanEvent, ImageScanReport] {

    // Tags that we are interested in because they are set on deployed images. We can ignore other tags (e.g. version
    // tags) because they represent old images or ones which have not been deployed yet.
    private val releaseTags = Set("latest", "intg", "staging", "prod", "mgmt")

    // Findings which should be included in alerts
    private val relevantFindingLevels = Set(
      FindingSeverity.CRITICAL,
      FindingSeverity.HIGH,
      FindingSeverity.MEDIUM,
      FindingSeverity.LOW,
      FindingSeverity.UNDEFINED
    )

    private val mutedVulnerabilities: Set[String] = eventConfig("alerts.ecr-scan.mute")
      .split(",")
      .toSet

    private val ecrScanDocumentationMessage: String = "See the TDR developer manual for guidance on fixing these vulnerabilities: " +
      "https://github.com/nationalarchives/tdr-dev-documentation/blob/master/manual/alerts/ecr-scans.md"

    private def slackBlock(text: String) = SlackBlock("section", SlackText("mrkdwn", text))

    private def includesReleaseTags(imageTags: List[String]): Boolean =
      imageTags.toSet.intersect(releaseTags).nonEmpty

    private def includesRelevantFindings(findings: Seq[Finding]): Boolean =
      findings.map(_.severity).toSet.intersect(relevantFindingLevels).nonEmpty

    private def shouldSendNotification(detail: ScanDetail, findings: Seq[Finding]): Boolean =
      includesReleaseTags(detail.tags) && includesRelevantFindings(findings)

    private def filterReport(report: ImageScanReport): ImageScanReport = {
      val filteredFindings = report.findings.filterNot(finding => mutedVulnerabilities.contains(finding.name))
      report.copy(findings = filteredFindings)
    }

    override def context(event: ScanEvent): IO[ImageScanReport] = {
      val repoName = event.detail.repositoryName
      val ecrClient = Clients.ecr(URI.create(ConfigFactory.load.getString("ecr.endpoint")))
      val ecrUtils: ECRUtils = ECRUtils(ecrClient)
      val findingsResponse = ecrUtils.imageScanFindings(repoName, event.detail.imageDigest)

      findingsResponse.map(response => {
        val findings = response.imageScanFindings.findings.asScala.map(finding => {
          Finding(finding.name, finding.severity)
        }).toSeq
        ImageScanReport(findings)
      })
    }

    override def slack(event: ScanEvent, report: ImageScanReport): Option[SlackMessage] = {
      val detail = event.detail
      val filteredReport = filterReport(report)
      if (shouldSendNotification(detail, filteredReport.findings)) {
        val headerBlock = slackBlock(s"*ECR image scan complete on image ${detail.repositoryName} ${detail.tags.mkString(",")}*")

        val criticalBlock = slackBlock(s"${filteredReport.criticalCount} critical severity vulnerabilities")
        val highBlock = slackBlock(s"${filteredReport.highCount} high severity vulnerabilities")
        val mediumBlock = slackBlock(s"${filteredReport.mediumCount} medium severity vulnerabilities")
        val lowBlock = slackBlock(s"${filteredReport.lowCount} low severity vulnerabilities")
        val undefinedBlock = slackBlock(s"${filteredReport.undefinedCount} undefined severity vulnerabilities")
        val documentationBlock = slackBlock(ecrScanDocumentationMessage)
        SlackMessage(List(headerBlock, criticalBlock, highBlock, mediumBlock, lowBlock, undefinedBlock, documentationBlock)).some
      } else {
        Option.empty
      }
    }

    override def email(event: ScanEvent, report: ImageScanReport): Option[SESUtils.Email] = {
      val detail = event.detail
      val filteredReport = filterReport(report)
      if (shouldSendNotification(detail, filteredReport.findings)) {
        val message = html(
          body(
            h1(s"Image scan results for ${detail.repositoryName}"),
            div(
              p(s"${filteredReport.criticalCount} critical vulnerabilities"),
              p(s"${filteredReport.highCount} high vulnerabilities"),
              p(s"${filteredReport.mediumCount} medium vulnerabilities"),
              p(s"${filteredReport.lowCount} low vulnerabilities"),
              p(s"${filteredReport.undefinedCount} undefined vulnerabilities")
            ),
            div(
              p(ecrScanDocumentationMessage)
            )
          )
        ).toString()
        Email("scanresults@tdr-management.nationalarchives.gov.uk", eventConfig("ses.email.to"), s"ECR scan results for ${detail.repositoryName}", message).some
      } else {
        Option.empty
      }
    }
  }

  implicit val maintenanceEventMessages: Messages[SSMMaintenanceEvent, Unit] = new Messages[SSMMaintenanceEvent, Unit] {
    override def context(event: SSMMaintenanceEvent): IO[Unit] = IO.unit

    override def email(scanDetail: SSMMaintenanceEvent, context: Unit): Option[Email] = Option.empty

    override def slack(scanDetail: SSMMaintenanceEvent, context: Unit): Option[SlackMessage] = {
      if (scanDetail.success) {
        None
      } else {
        SlackMessage(List(SlackBlock("section", SlackText("mrkdwn", "The Jenkins backup has failed. Please check the maintenance window in systems manager")))).some
      }
    }
  }

  implicit val exportStatusEventMessages: Messages[ExportStatusEvent, Unit] = new Messages[ExportStatusEvent, Unit] {
    override def context(event: ExportStatusEvent): IO[Unit] = IO.unit

    override def email(incomingEvent: ExportStatusEvent, context: Unit): Option[Email] = {
      logger.info(s"Skipping email for export complete message for consignment ${incomingEvent.consignmentId}")
      Option.empty
    }

    override def slack(incomingEvent: ExportStatusEvent, context: Unit): Option[SlackMessage] = {
      if(incomingEvent.environment != "intg" || !incomingEvent.success) {

        val exportInfoMessage = constructExportInfoMessage(incomingEvent)

        val message: String = if (incomingEvent.success) {
          ":white_check_mark: *Export success:* \n" +
            s"*Consignment ID:* ${incomingEvent.consignmentId} \n" +
            s"*Environment:* ${incomingEvent.environment}: \n" +
            s"$exportInfoMessage"
        } else {
          ":x: *Export failure:* \n" +
          s"*Consignment ID:* ${incomingEvent.consignmentId} \n" +
          s"*Environment:* ${incomingEvent.environment}: \n" +
          s"$exportInfoMessage"
        }
        SlackMessage(List(SlackBlock("section", SlackText("mrkdwn", message)))).some
      } else {
        Option.empty
      }
    }

    private def constructExportInfoMessage(incomingEvent: ExportStatusEvent): String = {
     if (incomingEvent.successDetails.isDefined) {
        val value = incomingEvent.successDetails.get
        s":\nUser ID: ${value.userId}" +
        s"\nConsignment Reference: ${value.consignmentReference}" +
        s"\nTransferring Body Code: ${value.transferringBodyCode}"
      } else if(incomingEvent.failureCause.isDefined) {
        s":\nCause: ${incomingEvent.failureCause.get}"
      } else ""
    }
  }
}

case class ImageScanReport(findings: Seq[Finding]) {
  def criticalCount: Int = findings.count(_.severity == FindingSeverity.CRITICAL)
  def highCount: Int = findings.count(_.severity == FindingSeverity.HIGH)
  def mediumCount: Int = findings.count(_.severity == FindingSeverity.MEDIUM)
  def lowCount: Int = findings.count(_.severity == FindingSeverity.LOW)
  def undefinedCount: Int = findings.count(_.severity == FindingSeverity.UNDEFINED)
}

case class Finding(name: String, severity: FindingSeverity)
