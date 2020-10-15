package uk.gov.nationalarchives.scannotifications.messages

import com.typesafe.config.ConfigFactory
import scalatags.Text.all._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.syntax.option._
import uk.gov.nationalarchives.aws.utils.SESUtils
import uk.gov.nationalarchives.aws.utils.SESUtils.Email
import uk.gov.nationalarchives.scannotifications.decoders.SSMMaintenanceDecoder.SSMMaintenanceEvent
import uk.gov.nationalarchives.scannotifications.decoders.ScanDecoder.ScanEvent

object EventMessages {

  case class SlackText(`type`: String, text: String)

  case class SlackBlock(`type`: String, text: SlackText)

  case class SlackMessage(blocks: List[SlackBlock])

  val scanEventMessages: Messages[ScanEvent] = new Messages[ScanEvent] {
    private def slackBlock(text: String) = SlackBlock("section", SlackText("mrkdwn", text))

    private def countBlock(count: Option[Int], level: String) = slackBlock(s"${count.getOrElse(0)} $level severity vulnerabilities")

    override def slack(event: ScanEvent) = {
      val detail = event.detail
      val headerBlock = slackBlock(s"*ECR image scan complete on image ${detail.repositoryName}*")
      val severityCounts = detail.findingSeverityCounts
      val criticalBlock = countBlock(severityCounts.critical, "critical")
      val highBlock = countBlock(severityCounts.high, "high")
      val mediumBlock = countBlock(severityCounts.medium, "medium")
      val lowBlock = countBlock(severityCounts.low, "low")
      SlackMessage(List(headerBlock, criticalBlock, highBlock, mediumBlock, lowBlock))
        .asJson.noSpaces.some
    }

    override def email(event: ScanEvent): Option[SESUtils.Email] = {
      val detail = event.detail
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
          )
        )
      ).toString()
      Email("scanresults@tdr-management.nationalarchives.gov.uk", ConfigFactory.load.getString("ses.email.to"), s"ECR scan results for ${detail.repositoryName}", message).some
    }
  }

  val maintenanceEventMessages: Messages[SSMMaintenanceEvent] = new Messages[SSMMaintenanceEvent] {
    override def email(scanDetail: SSMMaintenanceEvent): Option[Email] = Option.empty

    override def slack(scanDetail: SSMMaintenanceEvent): Option[String] = SlackMessage(List(SlackBlock("section", SlackText("mrkdwn", "The Jenkins backup has failed. Please check the maintenance window in systems manager")))).asJson.noSpaces.some
  }
}
