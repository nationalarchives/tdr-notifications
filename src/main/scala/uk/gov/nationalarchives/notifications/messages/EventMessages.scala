package uk.gov.nationalarchives.notifications.messages

import java.net.URI

import cats.effect.IO
import cats.syntax.all._
import com.typesafe.config.ConfigFactory
import io.circe.Encoder.AsObject.importedAsObjectEncoder
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import com.typesafe.scalalogging.Logger
import scalatags.Text.all._
import software.amazon.awssdk.services.ecr.model.FindingSeverity
import uk.gov.nationalarchives.aws.utils.Clients.{s3, s3Async}
import uk.gov.nationalarchives.aws.utils.SESUtils.Email
import uk.gov.nationalarchives.aws.utils.{Clients, ECRUtils, S3Utils, SESUtils}
import uk.gov.nationalarchives.notifications.decoders.DiskSpaceAlarmDecoder.DiskSpaceAlarmEvent
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportStatusEvent
import uk.gov.nationalarchives.notifications.decoders.KeycloakEventDecoder.KeycloakEvent
import uk.gov.nationalarchives.notifications.decoders.SSMMaintenanceDecoder.SSMMaintenanceEvent
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.{ScanDetail, ScanEvent}
import uk.gov.nationalarchives.notifications.decoders.TransformEngineRetryDecoder.TransformEngineRetryEvent
import uk.gov.nationalarchives.notifications.messages.Messages.eventConfig

import scala.jdk.CollectionConverters.CollectionHasAsScala

object EventMessages {
  val logger: Logger = Logger(this.getClass)

  trait ExportMessage {
    val consignmentReference: String
    val retryCount: Int
  }

  case class SlackText(`type`: String, text: String)

  case class SlackBlock(`type`: String, text: SlackText)

  case class SlackMessage(blocks: List[SlackBlock])

  case class SqsMessageDetails(queueUrl: String, messageBody: String)

  case class SqsExportMessage(packageSignedUrl: String,
                              packageShaSignedUrl: String,
                              consignmentReference: String,
                              retryCount: Int) extends ExportMessage

  private def generateSqsExportMessage(bucketName: String, consignmentRef: String, retryCount: Int): SqsMessageDetails = {
    val s3Utils = S3Utils(s3Async)
    val packageSignedUrl = s3Utils.generateGetObjectSignedUrl(bucketName, s"$consignmentRef.tar.gz").toString
    val packageShaSignedUrl = s3Utils.generateGetObjectSignedUrl(bucketName, s"$consignmentRef.tar.gz.sha256").toString
    val messageBody = SqsExportMessage(packageSignedUrl, packageShaSignedUrl, consignmentRef, retryCount).asJson.toString
    val queueUrl = eventConfig("sqs.queue.transform_engine_output")
    SqsMessageDetails(queueUrl, messageBody)
  }

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

    override def sqs(incomingEvent: ScanEvent, context: ImageScanReport): Option[SqsMessageDetails] = Option.empty
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

    override def sqs(incomingEvent: SSMMaintenanceEvent, context: Unit): Option[SqsMessageDetails] = Option.empty
  }

  implicit val exportStatusEventMessages: Messages[ExportStatusEvent, Unit] = new Messages[ExportStatusEvent, Unit] {
    private def sendToTransformEngine(ev: ExportStatusEvent): Boolean = {
      ev.success && ev.successDetails.exists(_.consignmentType == "judgment")
    }

    override def context(event: ExportStatusEvent): IO[Unit] = IO.unit

    override def email(incomingEvent: ExportStatusEvent, context: Unit): Option[Email] = {
      logger.info(s"Skipping email for export complete message for consignment ${incomingEvent.consignmentId}")
      Option.empty
    }

    override def slack(incomingEvent: ExportStatusEvent, context: Unit): Option[SlackMessage] = {
      if(incomingEvent.environment != "intg" || !incomingEvent.success) {

        val exportInfoMessage = constructExportInfoMessage(incomingEvent)

        val message: String = if (incomingEvent.success) {
          s":white_check_mark: Export *success* on *${incomingEvent.environment}!* \n" +
            s"*Consignment ID:* ${incomingEvent.consignmentId}" +
            s"$exportInfoMessage"
        } else {
          s":x: Export *failure* on *${incomingEvent.environment}!* \n" +
          s"*Consignment ID:* ${incomingEvent.consignmentId}" +
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
        s"\n*User ID:* ${value.userId}" +
        s"\n*Consignment Reference:* ${value.consignmentReference}" +
        s"\n*Transferring Body Name:* ${value.transferringBodyName}"
      } else if(incomingEvent.failureCause.isDefined) {
        s"\n*Cause:* ${incomingEvent.failureCause.get}"
      } else ""
    }

    override def sqs(incomingEvent: ExportStatusEvent, context: Unit): Option[SqsMessageDetails] = {
      if (sendToTransformEngine(incomingEvent)) {
        val value = incomingEvent.successDetails.get
        val consignmentReference = value.consignmentReference
        val bucketName = value.exportBucket
        Some(generateSqsExportMessage(bucketName, consignmentReference, 0))
      } else {
        None
      }
    }
  }

  implicit val keycloakEventMessages: Messages[KeycloakEvent, Unit] = new Messages[KeycloakEvent, Unit] {
    override def context(event: KeycloakEvent): IO[Unit] = IO.unit

    override def email(incomingEvent: KeycloakEvent, context: Unit): Option[Email] = {
      logger.info(s"Skipping email for Keycloak event ${incomingEvent}")
      Option.empty
    }

    override def slack(keycloakEvent: KeycloakEvent, context: Unit): Option[SlackMessage] = {
      if(keycloakEvent.tdrEnv == "intg") {
        Option.empty
      } else {
        SlackMessage(List(SlackBlock("section", SlackText("mrkdwn", s":warning: Keycloak Event ${keycloakEvent.tdrEnv}: ${keycloakEvent.message}")))).some
      }
    }

    override def sqs(incomingEvent: KeycloakEvent, context: Unit): Option[SqsMessageDetails] = Option.empty
  }

  implicit val diskSpaceAlarmMessages: Messages[DiskSpaceAlarmEvent, Unit] = new Messages[DiskSpaceAlarmEvent, Unit] {
    override def context(incomingEvent: DiskSpaceAlarmEvent): IO[Unit] = IO.unit

    override def email(incomingEvent: DiskSpaceAlarmEvent, context: Unit): Option[Email] = Option.empty

    override def slack(incomingEvent: DiskSpaceAlarmEvent, context: Unit): Option[SlackMessage] = {
      if(List("tdr-jenkins-disk-space-alarm-mgmt", "tdr-jenkins-prod-disk-space-alarm-mgmt").contains(incomingEvent.AlarmName)) {
        val extraBlocks = Nil
        val trigger = incomingEvent.Trigger
        trigger.Dimensions.find(d => d.name == "server_name").map(_.`value`).map(serverName => {
          if(incomingEvent.NewStateValue == "OK") {
            slackMessage(
              s":white_check_mark: $serverName disk space is now below ${trigger.Threshold} percent"
            )
          } else {
            if(incomingEvent.NewStateReason.contains("no datapoints were received")) {
              slackMessage(
                s":warning: $serverName is not sending disk space data to Cloudwatch. This is most likely because Jenkins is restarting."
              )
            } else {
              slackMessage(
                s":warning: $serverName disk space is over ${trigger.Threshold} percent",
                List("See <https://github.com/nationalarchives/tdr-dev-documentation/blob/master/manual/clear-jenkins-disk-space.md|the dev documentation> for details of how to clear disk space")
              )
            }
          }
        })
      } else {
        Option.empty
      }
    }

    private def slackMessage(text: String, extraBlocksText: List[String] = Nil): SlackMessage = {
      val slackBlocks = List(
        SlackBlock("section", SlackText("mrkdwn", text)),
        SlackBlock("section", SlackText("mrkdwn", "See <https://grafana.tdr-management.nationalarchives.gov.uk/d/eDVRAnI7z/jenkins-disk-space|this Grafana dashboard> to see the data"))
      ) ++ extraBlocksText.map(text => SlackBlock("section", SlackText("mrkdwn", text)))
      SlackMessage(slackBlocks)
    }

    override def sqs(incomingEvent: DiskSpaceAlarmEvent, context: Unit): Option[SqsMessageDetails] = Option.empty
  }

  implicit val transformEngineRetryMessages: Messages[TransformEngineRetryEvent, Unit] = new Messages[TransformEngineRetryEvent, Unit] {
    override def context(incomingEvent: TransformEngineRetryEvent): IO[Unit] = IO.unit

    override def email(incomingEvent: TransformEngineRetryEvent, context: Unit): Option[Email] = Option.empty

    override def slack(incomingEvent: TransformEngineRetryEvent, context: Unit): Option[SlackMessage] = Option.empty

    override def sqs(incomingEvent: TransformEngineRetryEvent, context: Unit): Option[SqsMessageDetails] = {
      //As only sending transform engine messages for judgments, will only get retry messages for the same
      val judgmentBucket = eventConfig("s3.judgment_export_bucket")
      val consignmentReference = incomingEvent.consignmentReference
      val retryCount = incomingEvent.retryCount
      Some(generateSqsExportMessage(judgmentBucket, consignmentReference, retryCount))
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
