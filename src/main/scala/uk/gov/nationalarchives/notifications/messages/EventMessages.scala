package uk.gov.nationalarchives.notifications.messages

import cats.effect.IO
import cats.syntax.all._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.circe.Encoder.AsObject.importedAsObjectEncoder
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import scalatags.Text.all._
import software.amazon.awssdk.services.ecr.model.FindingSeverity
import uk.gov.nationalarchives.aws.utils.Clients.s3Async
import uk.gov.nationalarchives.aws.utils.SESUtils.Email
import uk.gov.nationalarchives.aws.utils.{Clients, ECRUtils, S3Utils, SESUtils}
import uk.gov.nationalarchives.notifications.decoders.CloudwatchAlarmDecoder.CloudwatchAlarmEvent
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportStatusEvent
import uk.gov.nationalarchives.notifications.decoders.GenericMessageDecoder.GenericMessagesEvent
import uk.gov.nationalarchives.notifications.decoders.GovUkNotifyKeyRotationDecoder.GovUkNotifyKeyRotationEvent
import uk.gov.nationalarchives.notifications.decoders.KeycloakEventDecoder.KeycloakEvent
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.{ScanDetail, ScanEvent}
import uk.gov.nationalarchives.notifications.decoders.TransformEngineRetryDecoder.TransformEngineRetryEvent
import uk.gov.nationalarchives.notifications.decoders.TransformEngineV2Decoder._
import uk.gov.nationalarchives.notifications.messages.Messages.eventConfig

import java.net.URI
import java.net.URI
import java.sql.Timestamp
import java.time.Instant.now
import java.util.UUID
import scala.jdk.CollectionConverters.CollectionHasAsScala

object EventMessages {
  val logger: Logger = Logger(this.getClass)

  trait ExportMessage {
    val consignmentReference: String
    val consignmentType: String
  }

  trait TransformEngineSqsMessage {
    val `consignment-reference`: String
    val `consignment-type`: String
    val `number-of-retries`: Int
  }

  case class SlackText(`type`: String, text: String)

  case class SlackBlock(`type`: String, text: SlackText)

  case class SlackMessage(blocks: List[SlackBlock])

  case class SqsMessageDetails(queueUrl: String, messageBody: String)

  case class SnsMessageDetails(snsTopic: String, messageBody: String)

  case class SqsExportMessageBody(`consignment-reference`: String,
                                  `s3-bagit-url`: String,
                                  `s3-sha-url`: String,
                                  `consignment-type`: String,
                                  `number-of-retries`: Int) extends TransformEngineSqsMessage

  private def generateS3SignedUrl(bucketName: String, keyName: String): String = {
    val s3Utils = S3Utils(s3Async)
    s3Utils.generateGetObjectSignedUrl(bucketName, keyName).toString
  }

  private def generateSqsExportMessageBody(bucketName: String, exportMessage: ExportMessage): SqsMessageDetails = {
    val retryCount: Int = exportMessage match {
      case e: TransformEngineRetryEvent => e.numberOfRetries
      case _ => 0
    }

    val consignmentRef = exportMessage.consignmentReference
    val consignmentType = exportMessage.consignmentType
    val packageSignedUrl = generateS3SignedUrl(bucketName, s"$consignmentRef.tar.gz")
    val packageShaSignedUrl = generateS3SignedUrl(bucketName, s"$consignmentRef.tar.gz.sha256")
    val messageBody = SqsExportMessageBody(consignmentRef, packageSignedUrl, packageShaSignedUrl, consignmentType, retryCount).asJson.toString
    val queueUrl = eventConfig("sqs.queue.transform_engine_output")
    SqsMessageDetails(queueUrl, messageBody)
  }

  implicit val scanEventMessages: Messages[ScanEvent, ImageScanReport] = new Messages[ScanEvent, ImageScanReport] {

    // Tags that we are interested in because they are set on deployed images. We can ignore other tags (e.g. version
    // tags) because they represent old images or ones which have not been deployed yet.
    private val releaseTags = Set("latest", "intg", "staging", "prod", "mgmt")

    // Findings which should be included in slack alerts
    private val allFindingLevels = Set(
      FindingSeverity.CRITICAL,
      FindingSeverity.HIGH,
      FindingSeverity.MEDIUM,
      FindingSeverity.LOW,
      FindingSeverity.UNDEFINED
    )

    // Findings which should be included in email alerts
    private val highAndCriticalFindingLevels = Set(
      FindingSeverity.CRITICAL,
      FindingSeverity.HIGH
    )

    private val mutedVulnerabilities: Set[String] = eventConfig("alerts.ecr-scan.mute")
      .split(",")
      .toSet

    private val ecrScanDocumentationMessage: String = "See the TDR developer manual for guidance on fixing these vulnerabilities: " +
      "https://github.com/nationalarchives/tdr-dev-documentation/blob/master/manual/alerts/ecr-scans.md"

    private def slackBlock(text: String) = SlackBlock("section", SlackText("mrkdwn", text))

    private def includesReleaseTags(imageTags: List[String]): Boolean =
      imageTags.toSet.intersect(releaseTags).nonEmpty

    private def includesRelevantFindings(findings: Seq[Finding], findingLevels: Set[FindingSeverity]): Boolean = {
      findings.map(_.severity).toSet.intersect(findingLevels).nonEmpty
    }

    private def shouldSendSlackNotification(detail: ScanDetail, findings: Seq[Finding]): Boolean =
      includesReleaseTags(detail.tags) && includesRelevantFindings(findings, allFindingLevels)

    private def shouldSendEmailNotification(detail: ScanDetail, findings: Seq[Finding]): Boolean =
      includesReleaseTags(detail.tags) && includesRelevantFindings(findings, highAndCriticalFindingLevels)

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
      if (shouldSendSlackNotification(detail, filteredReport.findings)) {
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
      if (shouldSendEmailNotification(detail, filteredReport.findings)) {
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

    override def sns(incomingEvent: ScanEvent, context: ImageScanReport): Option[SnsMessageDetails] = Option.empty
  }

  implicit val exportStatusEventMessages: Messages[ExportStatusEvent, Unit] = new Messages[ExportStatusEvent, Unit] {
    private def sendToTransformEngine(ev: ExportStatusEvent): Boolean = {
      ev.success && ev.successDetails.exists(_.consignmentType == "judgment") && !ev.successDetails.exists(_.transferringBodyName.contains("MOCK"))
    }

    //For now only integration should send messages to TRE v2 until its deployed to the other TRE environments
    private def sendToTransformEngineV2(ev: ExportStatusEvent): Boolean = {
      ev.success && ev.environment == "intg" && !ev.successDetails.exists(_.transferringBodyName.contains("MOCK"))
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
        val successDetails = incomingEvent.successDetails.get
        val bucketName = successDetails.exportBucket
        Some(generateSqsExportMessageBody(bucketName, successDetails))
      } else {
        None
      }
    }

    override def sns(incomingEvent: ExportStatusEvent, context: Unit): Option[SnsMessageDetails] = {
      if (sendToTransformEngineV2(incomingEvent)) {
        val exportMessage = incomingEvent.successDetails.get
        val bucketName = exportMessage.exportBucket
        val consignmentRef = exportMessage.consignmentReference
        val consignmentType = exportMessage.consignmentType
        val uuids = List(Map("TDR-UUID" -> UUID.randomUUID.toString))
        val producer = Producer(incomingEvent.environment, "TDR", "tdr-export-process", "new-bagit", consignmentType)
        Some(generateSnsExportMessageBody(bucketName, consignmentRef, uuids, producer))
      } else {
        None
      }
    }
  }

  implicit val keycloakEventMessages: Messages[KeycloakEvent, Unit] = new Messages[KeycloakEvent, Unit] {
    override def context(event: KeycloakEvent): IO[Unit] = IO.unit

    override def email(incomingEvent: KeycloakEvent, context: Unit): Option[Email] = {
      logger.info(s"Skipping email for Keycloak event $incomingEvent")
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

    override def sns(incomingEvent: KeycloakEvent, context: Unit): Option[SnsMessageDetails] = Option.empty
  }

  implicit val transformEngineRetryMessages: Messages[TransformEngineRetryEvent, Unit] = new Messages[TransformEngineRetryEvent, Unit] {
    override def context(incomingEvent: TransformEngineRetryEvent): IO[Unit] = IO.unit

    override def email(incomingEvent: TransformEngineRetryEvent, context: Unit): Option[Email] = Option.empty

    override def slack(incomingEvent: TransformEngineRetryEvent, context: Unit): Option[SlackMessage] = Option.empty

    override def sqs(incomingEvent: TransformEngineRetryEvent, context: Unit): Option[SqsMessageDetails] = {
      //Will only receive judgment retry events as only sending judgment notification at the moment.
      if (incomingEvent.consignmentType == "judgment") {
        val judgmentBucket = eventConfig("s3.judgment_export_bucket")
        Some(generateSqsExportMessageBody(judgmentBucket, incomingEvent))
      } else None
    }

    override def sns(incomingEvent: TransformEngineRetryEvent, context: Unit): Option[SnsMessageDetails] = Option.empty
  }

  implicit val transformEngineV2RetryMessages: Messages[TransformEngineV2RetryEvent, Unit] = new Messages[TransformEngineV2RetryEvent, Unit] {
    override def context(incomingEvent: TransformEngineV2RetryEvent): IO[Unit] = IO.unit

    override def email(incomingEvent: TransformEngineV2RetryEvent, context: Unit): Option[Email] = Option.empty

    override def slack(incomingEvent: TransformEngineV2RetryEvent, context: Unit): Option[SlackMessage] = Option.empty

    override def sqs(incomingEvent: TransformEngineV2RetryEvent, context: Unit): Option[SqsMessageDetails] = Option.empty

    override def sns(incomingEvent: TransformEngineV2RetryEvent, context: Unit): Option[SnsMessageDetails] = {
      val consignmentRef: String = incomingEvent.parameters match {
        case p: ErrorParameters => p.`bagit-validation-error`.reference
      }

      val incomingProducer = incomingEvent.producer
      val bucketName = if (incomingProducer.`type` == "judgment") {
        eventConfig("s3.judgment_export_bucket")
      } else {
        eventConfig("s3.standard_export_bucket")
      }
      val uuids = incomingEvent.UUIDs :+ Map("TDR-UUID" -> UUID.randomUUID.toString)
      val producer = Producer(incomingProducer.environment, incomingProducer.name, incomingProducer.process, incomingProducer.`event-name`, incomingProducer.`type`)
      Some(generateSnsExportMessageBody(bucketName, consignmentRef, uuids, producer))
    }
  }

  implicit val genericRotationMessages: Messages[GenericMessagesEvent, Unit] = new Messages[GenericMessagesEvent, Unit] {
    override def context(incomingEvent: GenericMessagesEvent): IO[Unit] = IO.unit

    override def email(incomingEvent: GenericMessagesEvent, context: Unit): Option[Email] = Option.empty

    override def slack(incomingEvent: GenericMessagesEvent, context: Unit): Option[SlackMessage] = {
      val message = incomingEvent.messages.map(_.message).mkString("\n")
      SlackMessage(List(SlackBlock("section", SlackText("mrkdwn", message)))).some
    }

    override def sqs(incomingEvent: GenericMessagesEvent, context: Unit): Option[SqsMessageDetails] = Option.empty

    override def sns(incomingEvent: GenericMessagesEvent, context: Unit): Option[SnsMessageDetails] = Option.empty
  }

  implicit val cloudwatchAlarmMessages: Messages[CloudwatchAlarmEvent, Unit] = new Messages[CloudwatchAlarmEvent, Unit] {
    override def context(incomingEvent: CloudwatchAlarmEvent): IO[Unit] = IO.unit

    override def email(incomingEvent: CloudwatchAlarmEvent, context: Unit): Option[Email] = None

    override def slack(incomingEvent: CloudwatchAlarmEvent, context: Unit): Option[SlackMessage] = {
      val messageList = List(
        "*Cloudwatch Alarms*",
        s"Alarm state ${incomingEvent.NewStateValue}",
        s"Alarm triggered by ${incomingEvent.Trigger.MetricName}",
        s"Reason: ${incomingEvent.NewStateReason}",
        "",
        "*Dimensions affected*"
      ) ++ incomingEvent.Trigger.Dimensions.map(dimension => s"${dimension.name} - ${dimension.`value`}")
      SlackMessage(List(SlackBlock("section", SlackText("mrkdwn", messageList.mkString("\n"))))).some
    }

    override def sqs(incomingEvent: CloudwatchAlarmEvent, context: Unit): Option[SqsMessageDetails] = None

    override def sns(incomingEvent: CloudwatchAlarmEvent, context: Unit): Option[SnsMessageDetails] = None
  }

  private def generateSnsExportMessageBody(bucketName: String,
                                           consignmentRef: String,
                                           uuids: List[Map[String, String]],
                                           producer: Producer): SnsMessageDetails = {
    val topicArn = eventConfig("sns.topic.transform_engine_v2_in")
    val packageSignedUrl = generateS3SignedUrl(bucketName, s"$consignmentRef.tar.gz")
    val packageShaSignedUrl = generateS3SignedUrl(bucketName, s"$consignmentRef.tar.gz.sha256")
    val resource = Resource("Object", "url", packageSignedUrl)
    val resourceValidation = ResourceValidation("Object", "url", "SHA256", packageShaSignedUrl)
    val newBagit = NewBagit(resource, resourceValidation, consignmentRef)
    val parameters = NewBagitParameters(newBagit)
    val messageBody = TransferEngineV2NewBagitEvent("1.0.0", Timestamp.from(now).getTime, uuids, producer, parameters).asJson.toString()

    SnsMessageDetails(topicArn, messageBody)
  }

  implicit val govUkNotifyKeyRotationMessage: Messages[GovUkNotifyKeyRotationEvent, Unit] = new Messages[GovUkNotifyKeyRotationEvent, Unit] {
    override def context(incomingEvent: GovUkNotifyKeyRotationEvent): IO[Unit] = IO.unit

    override def email(incomingEvent: GovUkNotifyKeyRotationEvent, context: Unit): Option[Email] = None

    override def slack(incomingEvent: GovUkNotifyKeyRotationEvent, context: Unit): Option[SlackMessage] = {
      val ssmParameter: String = incomingEvent.detail.`parameter-name`
      val reason: String = incomingEvent.detail.`action-reason`
      val messageList = List(
        "*Rotate GOV.UK Notify API Key*",
        s"*$ssmParameter*: $reason",
        s"See here for instructions to rotate GOV.UK Notify API Keys: https://github.com/nationalarchives/tdr-dev-documentation-internal/blob/main/manual/govuk-notify.md#rotating-api-key"
      )
      SlackMessage(List(SlackBlock("section", SlackText("mrkdwn", messageList.mkString("\n"))))).some
    }

    override def sqs(incomingEvent: GovUkNotifyKeyRotationEvent, context: Unit): Option[SqsMessageDetails] = None

    override def sns(incomingEvent: GovUkNotifyKeyRotationEvent, context: Unit): Option[SnsMessageDetails] = None
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
