package uk.gov.nationalarchives.notifications

import cats.implicits.catsSyntaxOptionId
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.nationalarchives.notifications.EcrScanIntegrationSpec.scanEventInputText
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.{ScanDetail, ScanEvent, ScanFindingCounts}

import scala.io.Source

class EcrScanIntegrationSpec extends LambdaIntegrationSpec with MockEcrApi {

  override lazy val events: Seq[Event] = Seq(
    Event(
      description = "an ECR scan of 'latest' with a mix of severities",
      input = scanEventInputText(mixedSeverityEvent),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackBody(mixedSeverityEvent, ExpectedFindings(1, 2, 24, 4, 1).some), webhookUrl = "/webhook-url"))
      ),
      stubContext = stubEcrApiResponse(mixedSeverityEvent.detail.imageDigest, mixedSeverityFindings)
    ),
    Event(
      description = "an ECR scan of 'latest' with only medium severity vulnerabilities",
      input = scanEventInputText(mediumSeverityEvent),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackBody(mediumSeverityEvent, ExpectedFindings(0, 0, 1, 0, 0).some), webhookUrl = "/webhook-url"))
      ),
      stubContext = stubEcrApiResponse(mediumSeverityEvent.detail.imageDigest, mediumSeverityFindings)
    ),
    Event(
      description = "an ECR scan of 'latest' with only low severity vulnerabilities",
      input = scanEventInputText(lowSeverityEvent),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackBody(lowSeverityEvent, ExpectedFindings(0, 0, 0, 1, 0).some), webhookUrl = "/webhook-url"))
      ),
      stubContext = stubEcrApiResponse(lowSeverityEvent.detail.imageDigest, lowSeverityFindings)
    ),
    Event(
      description = "an ECR scan of 'latest' with only undefined severity vulnerabilities",
      input = scanEventInputText(undefinedSeverityEvent),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackBody(undefinedSeverityEvent, ExpectedFindings(0, 0, 0, 0, 1).some), webhookUrl = "/webhook-url"))
      ),
      stubContext = stubEcrApiResponse(undefinedSeverityEvent.detail.imageDigest, undefinedFindings)
    ),
    Event(
      description = "an ECR scan of 'latest' with only informational vulnerabilities",
      input = scanEventInputText(informationalEvent),
      expectedOutput = ExpectedOutput(),
      stubContext = stubEcrApiResponse(informationalEvent.detail.imageDigest, informationalFindings)
    ),
    Event(
      description = "an ECR scan of 'latest' with no results",
      input = scanEventInputText(noVulnerabilitiesEvent),
      expectedOutput = ExpectedOutput(),
      stubContext = stubEcrApiResponse(noVulnerabilitiesEvent.detail.imageDigest, noFindings)
    ),
    Event(
      description = "an ECR scan of an image which fails",
      input = scanEventInputText(ecrScanFailedEvent),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackBody(ecrScanFailedEvent, None), webhookUrl = "/webhook-url"))
      ),
      stubContext = stubEcrApiResponse(ecrScanFailedEvent.detail.imageDigest, ecrScanFailed)
    ),
    Event(
      description = "an ECR scan of an image with a non-deployment tag",
      input = scanEventInputText(otherTagEvent),
      expectedOutput = ExpectedOutput(),
      stubContext = stubEcrApiResponse(otherTagEvent.detail.imageDigest, lowSeverityFindings)
    ),
    Event(
      description = "an ECR scan of 'intg' with no results",
      input = scanEventInputText(intgTagEmptyEvent),
      expectedOutput = ExpectedOutput(),
      stubContext = stubEcrApiResponse(intgTagEmptyEvent.detail.imageDigest, noFindings)
    ),
    Event(
      description = "an ECR scan which only contains a muted vulnerability",
      input = scanEventInputText(lowSeverityEvent),
      expectedOutput = ExpectedOutput(),
      stubContext = stubEcrApiResponse(lowSeverityEvent.detail.imageDigest, findingsWithOnlyMutedVulnerability)
    ),
    Event(
      description = "an ECR scan with a mix of muted and non-muted vulnerabilities",
      input = scanEventInputText(mixedSeverityEvent),
      expectedOutput = ExpectedOutput(
        slackMessage = Some(SlackMessage(body = expectedSlackBody(mixedSeverityEvent, ExpectedFindings(1, 2, 24, 3, 0).some), webhookUrl = "/webhook-url"))
      ),
      stubContext = stubEcrApiResponse(mixedSeverityEvent.detail.imageDigest, findingsIncludingMutedVulnerability)
    )
  )

  private lazy val mixedSeverityEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("latest"), "abcd1", mixedCounts, "COMPLETE"))
  private lazy val lowSeverityEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("latest"), "abcd2", lowSeverityCounts, "COMPLETE"))
  private lazy val mediumSeverityEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("latest"), "abcd2", mediumSeverityCounts, "COMPLETE"))
  private lazy val informationalEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("latest"), "1234", informationalCounts, "COMPLETE"))
  private lazy val undefinedSeverityEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("latest"), "1234", undefinedSeverityCounts, "COMPLETE"))
  private lazy val noVulnerabilitiesEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("latest"), "abcd3", emptyCounts, "COMPLETE"))
  private lazy val otherTagEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("anothertag"), "abcd4", emptyCounts, "COMPLETE"))
  private lazy val intgTagEmptyEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("intg"), "abcd5", zeroCounts, "COMPLETE"))
  private lazy val ecrScanFailedEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("latest"), "abcd6", None, "FAILED"))

  private lazy val mixedCounts = ScanFindingCounts(10, 100, 1000, 10000, 10, 1).some
  private lazy val lowSeverityCounts = ScanFindingCounts(0, 0, 0, 10, 0, 0).some
  private lazy val mediumSeverityCounts = ScanFindingCounts(0, 0, 10, 0, 0, 0).some
  private lazy val informationalCounts = ScanFindingCounts(0, 0, 0, 0, 10, 0).some
  private lazy val undefinedSeverityCounts = ScanFindingCounts(0, 0, 0, 0, 10, 0).some
  private lazy val emptyCounts = ScanFindingCounts(0, 0, 0, 0, 0, 0).some
  private lazy val zeroCounts = ScanFindingCounts(0, 0, 0, 0, 0, 0).some

  private lazy val mixedSeverityFindings = Source.fromResource("ecr-findings/mixed-severity.json").getLines.mkString
  private lazy val mediumSeverityFindings = Source.fromResource("ecr-findings/medium-severity.json").getLines.mkString
  private lazy val lowSeverityFindings = Source.fromResource("ecr-findings/low-severity.json").getLines.mkString
  private lazy val undefinedFindings = Source.fromResource("ecr-findings/undefined-severity.json").getLines.mkString
  private lazy val informationalFindings = Source.fromResource("ecr-findings/informational.json").getLines.mkString
  private lazy val noFindings = Source.fromResource("ecr-findings/no-findings.json").getLines.mkString
  private lazy val ecrScanFailed = Source.fromResource("ecr-findings/ecr-scan-failed.json").getLines.mkString
  private lazy val findingsWithOnlyMutedVulnerability = Source.fromResource("ecr-findings/muted-vulnerability.json").getLines.mkString
  private lazy val findingsIncludingMutedVulnerability = Source.fromResource("ecr-findings/including-muted-vulnerability.json").getLines.mkString

  case class ExpectedFindings(critical: Int, high: Int, medium: Int, low: Int, undefined: Int)

  private def stubEcrApiResponse(sha256Digest: String, response: String): () => Unit = () => {
    ecrApiEndpoint.stubFor(post(urlEqualTo("/"))
      .withRequestBody(matchingJsonPath("$.repositoryName", equalTo("repo-name")))
      .withRequestBody(matchingJsonPath("$.imageId.imageDigest", equalTo(sha256Digest)))
      .willReturn(ok(response))
    )
  }

  private def expectedSlackBody(scanEvent: ScanEvent, expectedFindings: Option[ExpectedFindings] = None): String = {
    if (scanEvent.detail.scanStatus == "FAILED") {
      s"""
         |{
         |  "blocks" : [ {
         |    "type" : "section",
         |    "text" : {
         |      "type" : "mrkdwn",
         |      "text" : "*ECR image scan FAILED on image ${scanEvent.detail.repositoryName} ${scanEvent.detail.tags.mkString(",")}*"
         |    }
         |  }]
         |}
         |""".stripMargin
    } else {
      s"""
         |{
         |  "blocks" : [ {
         |    "type" : "section",
         |    "text" : {
         |      "type" : "mrkdwn",
         |      "text" : "*ECR image scan complete on image ${scanEvent.detail.repositoryName} ${scanEvent.detail.tags.mkString(",")}*"
         |    }
         |  }, {
         |    "type" : "section",
         |    "text" : {
         |      "type" : "mrkdwn",
         |      "text" : "${expectedFindings.get.critical} critical severity vulnerabilities"
         |    }
         |  }, {
         |    "type" : "section",
         |    "text" : {
         |      "type" : "mrkdwn",
         |      "text" : "${expectedFindings.get.high} high severity vulnerabilities"
         |    }
         |  }, {
         |    "type" : "section",
         |    "text" : {
         |      "type" : "mrkdwn",
         |      "text" : "${expectedFindings.get.medium} medium severity vulnerabilities"
         |    }
         |  }, {
         |    "type" : "section",
         |    "text" : {
         |      "type" : "mrkdwn",
         |      "text" : "${expectedFindings.get.low} low severity vulnerabilities"
         |    }
         |  }, {
         |    "type" : "section",
         |    "text" : {
         |      "type" : "mrkdwn",
         |      "text" : "${expectedFindings.get.undefined} undefined severity vulnerabilities"
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
}

object EcrScanIntegrationSpec {
  private def getCounts(scanEvent: ScanEvent): (Int, Int, Int, Int, Int) = {
    val critical = scanEvent.detail.findingSeverityCounts.map(_.critical).getOrElse(0)
    val high = scanEvent.detail.findingSeverityCounts.map(_.high).getOrElse(0)
    val medium = scanEvent.detail.findingSeverityCounts.map(_.medium).getOrElse(0)
    val low = scanEvent.detail.findingSeverityCounts.map(_.low).getOrElse(0)
    val undefined = scanEvent.detail.findingSeverityCounts.map(_.undefined).getOrElse(0)
    (critical, high, medium, low, undefined)
  }

  def scanEventInputText(scanEvent: ScanEvent): String = {
    val (critical, high, medium, low, undefined) = getCounts(scanEvent)
    if (scanEvent.detail.findingSeverityCounts.isDefined) {
      s"""
         |{
         |  "detail": {
         |    "scan-status": "${scanEvent.detail.scanStatus}",
         |    "repository-name": "${scanEvent.detail.repositoryName}",
         |    "image-tags": ["${scanEvent.detail.tags.mkString("\",\"")}"],
         |    "image-digest": "${scanEvent.detail.imageDigest}",
         |    "finding-severity-counts": {
         |      "CRITICAL": $critical,
         |      "HIGH": $high,
         |      "MEDIUM": $medium,
         |      "LOW": $low,
         |      "UNDEFINED": $undefined
         |    }
         |  }
         |}
         |""".stripMargin
    } else {
      s"""
         |{
         |  "detail": {
         |    "scan-status": "${scanEvent.detail.scanStatus}",
         |    "repository-name": "${scanEvent.detail.repositoryName}",
         |    "image-tags": ["${scanEvent.detail.tags.mkString("\",\"")}"],
         |    "image-digest": "${scanEvent.detail.imageDigest}"
         |  }
         |}
         |""".stripMargin
    }
  }
}
