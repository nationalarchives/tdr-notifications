package uk.gov.nationalarchives.notifications

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.prop.TableFor5
import uk.gov.nationalarchives.notifications.EcrScanIntegrationSpec.scanEventInputText
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.{ScanDetail, ScanEvent, ScanFindingCounts}

import scala.io.Source

class EcrScanIntegrationSpec extends LambdaIntegrationSpec with MockEcrApi {

  override lazy val events: TableFor5[String, String, Option[String], Option[String], () => ()] = Table(
    ("description", "input", "emailBody", "slackBody", "stubContext"),
    (
      "an ECR scan of 'latest' with a mix of severities",
      scanEventInputText(mixedSeverityEvent),
      Some(expectedEmailBody(mixedSeverityEvent, ExpectedFindings(1, 2, 24, 4, 1))),
      Some(expectedSlackBody(mixedSeverityEvent, ExpectedFindings(1, 2, 24, 4, 1))),
      stubEcrApiResponse(mixedSeverityEvent.detail.imageDigest, mixedSeverityFindings)
    ),
    (
      "an ECR scan of 'latest' with only low severity vulnerabilities",
      scanEventInputText(lowSeverityEvent),
      Some(expectedEmailBody(lowSeverityEvent, ExpectedFindings(0, 0, 0, 1, 0))),
      Some(expectedSlackBody(lowSeverityEvent, ExpectedFindings(0, 0, 0, 1, 0))),
      stubEcrApiResponse(lowSeverityEvent.detail.imageDigest, lowSeverityFindings)
    ),
    (
      "an ECR scan of 'latest' with only undefined severity vulnerabilities",
      scanEventInputText(undefinedSeverityEvent),
      Some(expectedEmailBody(undefinedSeverityEvent, ExpectedFindings(0, 0, 0, 0, 1))),
      Some(expectedSlackBody(undefinedSeverityEvent, ExpectedFindings(0, 0, 0, 0, 1))),
      stubEcrApiResponse(undefinedSeverityEvent.detail.imageDigest, undefinedFindings)
    ),
    (
      "an ECR scan of 'latest' with only informational vulnerabilities",
      scanEventInputText(informationalEvent),
      None,
      None,
      stubEcrApiResponse(informationalEvent.detail.imageDigest, informationalFindings)
    ),
    (
      "an ECR scan of 'latest' with no results",
      scanEventInputText(noVulnerabilitiesEvent),
      None,
      None,
      stubEcrApiResponse(noVulnerabilitiesEvent.detail.imageDigest, noFindings)
    ),
    (
      "an ECR scan of an image with a non-deployment tag",
      scanEventInputText(otherTagEvent),
      None,
      None,
      stubEcrApiResponse(otherTagEvent.detail.imageDigest, lowSeverityFindings)
    ),
    (
      "an ECR scan of 'intg' with no results",
      scanEventInputText(intgTagEmptyEvent),
      None,
      None,
      stubEcrApiResponse(intgTagEmptyEvent.detail.imageDigest, noFindings)
    ),
    (
      "an ECR scan which only contains a muted vulnerability",
      scanEventInputText(lowSeverityEvent),
      None,
      None,
      stubEcrApiResponse(lowSeverityEvent.detail.imageDigest, findingsWithOnlyMutedVulnerability)
    ),
    (
      "an ECR scan with a mix of muted and non-muted vulnerabilities",
      scanEventInputText(mixedSeverityEvent),
      Some(expectedEmailBody(mixedSeverityEvent, ExpectedFindings(1, 2, 24, 3, 0))),
      Some(expectedSlackBody(mixedSeverityEvent, ExpectedFindings(1, 2, 24, 3, 0))),
      stubEcrApiResponse(mixedSeverityEvent.detail.imageDigest, findingsIncludingMutedVulnerability)
    ),
  )

  private lazy val mixedSeverityEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("latest"), "abcd1", mixedCounts))
  private lazy val lowSeverityEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("latest"), "abcd2", lowSeverityCounts))
  private lazy val informationalEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("latest"), "1234", informationalCounts))
  private lazy val undefinedSeverityEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("latest"), "1234", undefinedSeverityCounts))
  private lazy val noVulnerabilitiesEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("latest"), "abcd3", emptyCounts))
  private lazy val otherTagEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("anothertag"), "abcd4", emptyCounts))
  private lazy val intgTagEmptyEvent: ScanEvent = ScanEvent(ScanDetail("repo-name", List("intg"), "abcd5", zeroCounts))

  private lazy val mixedCounts = ScanFindingCounts(10, 100, 1000, 10000, 10, 1)
  private lazy val lowSeverityCounts = ScanFindingCounts(0, 0, 0, 10, 0, 0)
  private lazy val informationalCounts = ScanFindingCounts(0, 0, 0, 0, 10, 0)
  private lazy val undefinedSeverityCounts = ScanFindingCounts(0, 0, 0, 0, 10, 0)
  private lazy val emptyCounts = ScanFindingCounts(0, 0, 0, 0, 0, 0)
  private lazy val zeroCounts = ScanFindingCounts(0, 0, 0, 0, 0, 0)

  private lazy val mixedSeverityFindings = Source.fromResource("ecr-findings/mixed-severity.json").getLines.mkString
  private lazy val lowSeverityFindings = Source.fromResource("ecr-findings/low-severity.json").getLines.mkString
  private lazy val undefinedFindings = Source.fromResource("ecr-findings/undefined-severity.json").getLines.mkString
  private lazy val informationalFindings = Source.fromResource("ecr-findings/informational.json").getLines.mkString
  private lazy val noFindings = Source.fromResource("ecr-findings/no-findings.json").getLines.mkString
  private lazy val findingsWithOnlyMutedVulnerability = Source.fromResource("ecr-findings/muted-vulnerability.json").getLines.mkString
  private lazy val findingsIncludingMutedVulnerability = Source.fromResource("ecr-findings/including-muted-vulnerability.json").getLines.mkString

  case class ExpectedFindings(critical: Int, high: Int, medium: Int, low: Int, undefined: Int)

  private def stubEcrApiResponse(sha256Digest: String, response: String): () => () = () => {
    ecrApiEndpoint.stubFor(post(urlEqualTo("/"))
      .withRequestBody(matchingJsonPath("$.repositoryName", equalTo("repo-name")))
      .withRequestBody(matchingJsonPath("$.imageId.imageDigest", equalTo(sha256Digest)))
      .willReturn(ok(response))
    )
  }

  private def expectedEmailBody(scanEvent: ScanEvent, expectedFindings: ExpectedFindings): String = {
    "Action=SendEmail&Version=2010-12-01&Source=scanresults%40tdr-management.nationalarchives.gov.uk" +
      "&Destination.ToAddresses.member.1=aws_tdr_management%40nationalarchives.gov.uk" +
      s"&Message.Subject.Data=ECR+scan+results+for+${scanEvent.detail.repositoryName}&Message.Subject.Charset=UTF-8" +
      s"&Message.Body.Html.Data=%3Chtml%3E%3Cbody%3E%3Ch1%3EImage+scan+results+for+${scanEvent.detail.repositoryName}%3C%2Fh1%3E%3Cdiv%3E%3Cp%3E" +
      s"${expectedFindings.critical}+critical+vulnerabilities%3C%2Fp%3E%3Cp%3E" +
      s"${expectedFindings.high}+high+vulnerabilities%3C%2Fp%3E%3Cp%3E" +
      s"${expectedFindings.medium}+medium+vulnerabilities%3C%2Fp%3E%3Cp%3E" +
      s"${expectedFindings.low}+low+vulnerabilities%3C%2Fp%3E%3Cp%3E" +
      s"${expectedFindings.undefined}+undefined+vulnerabilities%3C%2Fp%3E%3C%2Fdiv%3E" +
      "%3Cdiv%3E%3Cp%3E" +
      "See+the+TDR+developer+manual+for+guidance+on+fixing+these+vulnerabilities%3A+" +
      "https%3A%2F%2Fgithub.com%2Fnationalarchives%2Ftdr-dev-documentation%2Fblob%2Fmaster%2Fmanual%2Falerts%2Fecr-scans.md" +
      "%3C%2Fp%3E%3C%2Fdiv%3E" +
      "%3C%2Fbody%3E%3C%2Fhtml%3E" +
      "&Message.Body.Html.Charset=UTF-8"
  }

  private def expectedSlackBody(scanEvent: ScanEvent, expectedFindings: ExpectedFindings): String = {
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
       |      "text" : "${expectedFindings.critical} critical severity vulnerabilities"
       |    }
       |  }, {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : "${expectedFindings.high} high severity vulnerabilities"
       |    }
       |  }, {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : "${expectedFindings.medium} medium severity vulnerabilities"
       |    }
       |  }, {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : "${expectedFindings.low} low severity vulnerabilities"
       |    }
       |  }, {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : "${expectedFindings.undefined} undefined severity vulnerabilities"
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
  private def getCounts(scanEvent: ScanEvent): (Int, Int, Int, Int, Int) = {
    val critical = scanEvent.detail.findingSeverityCounts.critical
    val high = scanEvent.detail.findingSeverityCounts.high
    val medium = scanEvent.detail.findingSeverityCounts.medium
    val low = scanEvent.detail.findingSeverityCounts.low
    val undefined = scanEvent.detail.findingSeverityCounts.undefined
    (critical, high, medium, low, undefined)
  }

  def scanEventInputText(scanEvent: ScanEvent): String = {
    val (critical, high, medium, low, undefined) = getCounts(scanEvent)
    s"""
       |{
       |  "detail": {
       |    "scan-status": "COMPLETE",
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
  }
}
