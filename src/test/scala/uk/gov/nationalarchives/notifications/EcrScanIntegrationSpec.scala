package uk.gov.nationalarchives.notifications

import org.scalatest.prop.TableFor3
import uk.gov.nationalarchives.notifications.EcrScanIntegrationSpec.{getCounts, scanEventInputText}
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.{ScanDetail, ScanEvent, ScanFindingCounts}

class EcrScanIntegrationSpec extends LambdaIntegrationSpec {
  override lazy val events: TableFor3[String, Option[String], Option[String]] = Table(
    ("input", "emailBody", "slackBody"),
    (scanEventInputText(scanEvent1), Some(expectedEmailBody(scanEvent1)), Some(expectedSlackBody(scanEvent1))),
    (scanEventInputText(scanEvent2), Some(expectedEmailBody(scanEvent2)), Some(expectedSlackBody(scanEvent2))),
    (scanEventInputText(scanEvent3), None, None),
    (scanEventInputText(scanEvent4), None, None),
    (scanEventInputText(scanEvent5), None, None)
  )

  private lazy val scanEvent1: ScanEvent = ScanEvent(ScanDetail("", List("latest"), ScanFindingCounts(Some(10), Some(100), Some(1000), Some(10000))))
  private lazy val scanEvent2: ScanEvent = ScanEvent(ScanDetail("", List("latest"), ScanFindingCounts(Option.empty, Some(0), Option.empty, Some(10))))
  private lazy val scanEvent3: ScanEvent = ScanEvent(ScanDetail("", List("latest"), ScanFindingCounts(Option.empty, Option.empty, Option.empty, Option.empty)))
  private lazy val scanEvent4: ScanEvent = ScanEvent(ScanDetail("", List("anothertag"), ScanFindingCounts(Option.empty, Option.empty, Option.empty, Option.empty)))
  private lazy val scanEvent5: ScanEvent = ScanEvent(ScanDetail("", List("intg"), ScanFindingCounts(Some(0), Some(0), Some(0), Some(0))))

  private def expectedEmailBody(scanEvent: ScanEvent): String = {
    val (critical, high, medium, low) = getCounts(scanEvent)
    "Action=SendEmail&Version=2010-12-01&Source=scanresults%40tdr-management.nationalarchives.gov.uk" +
      "&Destination.ToAddresses.member.1=aws_tdr_management%40nationalarchives.gov.uk" +
      "&Message.Subject.Data=ECR+scan+results+for+yara-dependencies&Message.Subject.Charset=UTF-8" +
      "&Message.Body.Html.Data=%3Chtml%3E%3Cbody%3E%3Ch1%3EImage+scan+results+for+yara-dependencies%3C%2Fh1%3E%3Cdiv%3E%3Cp%3E" +
      s"$critical+critical+vulnerabilities%3C%2Fp%3E%3Cp%3E" +
      s"$high+high+vulnerabilities%3C%2Fp%3E%3Cp%3E" +
      s"$medium+medium+vulnerabilities%3C%2Fp%3E%3Cp%3E" +
      s"$low+low+vulnerabilities%3C%2Fp%3E%3C%2Fdiv%3E" +
      "%3Cdiv%3E%3Cp%3E" +
      "See+the+TDR+developer+manual+for+guidance+on+fixing+these+vulnerabilities%3A+" +
      "https%3A%2F%2Fgithub.com%2Fnationalarchives%2Ftdr-dev-documentation%2Fblob%2Fmaster%2Fmanual%2Falerts%2Fecr-scans.md" +
      "%3C%2Fp%3E%3C%2Fdiv%3E" +
      "%3C%2Fbody%3E%3C%2Fhtml%3E" +
      "&Message.Body.Html.Charset=UTF-8"
  }

  private def expectedSlackBody(scanEvent: ScanEvent): String = {
    val (critical, high, medium, low) = getCounts(scanEvent)
    s"""
       |{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : "*ECR image scan complete on image yara-dependencies ${scanEvent.detail.tags.mkString(",")}*"
       |    }
       |  }, {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : "$critical critical severity vulnerabilities"
       |    }
       |  }, {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : "$high high severity vulnerabilities"
       |    }
       |  }, {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : "$medium medium severity vulnerabilities"
       |    }
       |  }, {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : "$low low severity vulnerabilities"
       |    }
       |  }, {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : "See the TDR developer manual for guidance on fixing these vulnerabilities: https://github.com/nationalarchives/tdr-dev-documentation/blob/master/manual/alerts/ecr-scans.md"
       |    }
       |  } ]
       |}
       |""".stripMargin
  }
}

object EcrScanIntegrationSpec {
  private def getCounts(scanEvent: ScanEvent): (Int, Int, Int, Int) = {
    val critical = scanEvent.detail.findingSeverityCounts.critical.getOrElse(0)
    val high = scanEvent.detail.findingSeverityCounts.high.getOrElse(0)
    val medium = scanEvent.detail.findingSeverityCounts.medium.getOrElse(0)
    val low = scanEvent.detail.findingSeverityCounts.low.getOrElse(0)
    (critical, high, medium, low)
  }

  def scanEventInputText(scanEvent: ScanEvent): String = {
    val (critical, high, medium, low) = getCounts(scanEvent)
    s"""
       |{
       |  "detail": {
       |    "scan-status": "COMPLETE",
       |    "repository-name": "yara-dependencies",
       |    "image-tags" : ["${scanEvent.detail.tags.mkString("\",\"")}"],
       |    "finding-severity-counts": {
       |      "CRITICAL": $critical,
       |      "HIGH": $high,
       |      "MEDIUM": $medium,
       |      "LOW": $low
       |    }
       |  }
       |}
       |""".stripMargin
  }
}
