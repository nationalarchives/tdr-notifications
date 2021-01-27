package uk.gov.nationalarchives.notifications.messages

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import scalatags.Text.all._
import cats.implicits._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import uk.gov.nationalarchives.aws.utils.SESUtils
import uk.gov.nationalarchives.aws.utils.SESUtils.Email
import uk.gov.nationalarchives.notifications.decoders.ExportStatusDecoder.ExportStatusEvent
import uk.gov.nationalarchives.notifications.decoders.SSMMaintenanceDecoder.SSMMaintenanceEvent
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.{ScanDetail, ScanEvent}

object EventMessages {

  implicit def logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  case class SlackText(`type`: String, text: String)

  case class SlackBlock(`type`: String, text: SlackText)

  case class SlackMessage(blocks: List[SlackBlock])

  implicit val scanEventMessages: Messages[ScanEvent] = new Messages[ScanEvent] {
    private val allowedTags = Set("latest", "intg", "staging", "prod", "mgmt")

    private def slackBlock(text: String) = SlackBlock("section", SlackText("mrkdwn", text))

    private def countBlock(count: Option[Int], level: String) = slackBlock(s"${count.getOrElse(0)} $level severity vulnerabilities")

    private def allowedTagMatches(detail: ScanDetail): Set[String] = detail.tags.toSet.intersect(allowedTags)

    private def shouldSendNotification(detail: ScanDetail): Boolean = allowedTagMatches(detail).nonEmpty && !detail.findingSeverityCounts.areAllZero()

    override def slack(event: ScanEvent): Option[SlackMessage] = {
      val detail = event.detail
      if(shouldSendNotification(detail)) {
        val headerBlock = slackBlock(s"*ECR image scan complete on image ${detail.repositoryName} ${detail.tags.mkString(",")}*")
        val severityCounts = detail.findingSeverityCounts
        val criticalBlock = countBlock(severityCounts.critical, "critical")
        val highBlock = countBlock(severityCounts.high, "high")
        val mediumBlock = countBlock(severityCounts.medium, "medium")
        val lowBlock = countBlock(severityCounts.low, "low")
        val documentationBlock = slackBlock("See the TDR developer manual for guidance on fixing these vulnerabilities: " +
          "https://github.com/nationalarchives/tdr-dev-documentation/blob/master/manual/alerts/ecr-scans.md")
        SlackMessage(List(headerBlock, criticalBlock, highBlock, mediumBlock, lowBlock, documentationBlock)).some
      } else {
        Option.empty
      }
    }

    override def email(event: ScanEvent): Option[SESUtils.Email] = {
      val detail = event.detail
      if(shouldSendNotification(detail)) {
        val critical = detail.findingSeverityCounts.critical.getOrElse(0)
        val high = detail.findingSeverityCounts.high.getOrElse(0)
        val medium = detail.findingSeverityCounts.medium.getOrElse(0)
        val low = detail.findingSeverityCounts.low.getOrElse(0)

        val message = html(
          body(
            h1(s"Image scan results for ${detail.repositoryName}"),
            div(
              p(s"$critical critical vulnerabilities"),
              p(s"$high high vulnerabilities"),
              p(s"$medium medium vulnerabilities"),
              p(s"$low low vulnerabilities")
            ),
            div(
              p("See the TDR developer manual for guidance on fixing these vulnerabilities: " +
                "https://github.com/nationalarchives/tdr-dev-documentation/blob/master/manual/alerts/ecr-scans.md")
            )
          )
        ).toString()
        Email("scanresults@tdr-management.nationalarchives.gov.uk", ConfigFactory.load.getString("ses.email.to"), s"ECR scan results for ${detail.repositoryName}", message).some
      } else {
        Option.empty
      }
    }
  }

  implicit val maintenanceEventMessages: Messages[SSMMaintenanceEvent] = new Messages[SSMMaintenanceEvent] {
    override def email(scanDetail: SSMMaintenanceEvent): Option[Email] = Option.empty

    override def slack(scanDetail: SSMMaintenanceEvent): Option[SlackMessage] = {
      if (scanDetail.success) {
        None
      } else {
        SlackMessage(List(SlackBlock("section", SlackText("mrkdwn", "The Jenkins backup has failed. Please check the maintenance window in systems manager")))).some
      }
    }
  }

  implicit val exportStatusEventMessages: Messages[ExportStatusEvent] = new Messages[ExportStatusEvent] {
    override def email(incomingEvent: ExportStatusEvent): Option[Email] = {
      logger.info(s"Skipping email for export complete message for consignment ${incomingEvent.consignmentId}")
      Option.empty
    }

    override def slack(incomingEvent: ExportStatusEvent): Option[SlackMessage] = {
      val message = s"The export for the consignment ${incomingEvent.consignmentId} has ${if (incomingEvent.success) "completed" else "failed"} for environment ${incomingEvent.environment}"
      SlackMessage(List(SlackBlock("section", SlackText("mrkdwn", message)))).some
    }
  }
}
