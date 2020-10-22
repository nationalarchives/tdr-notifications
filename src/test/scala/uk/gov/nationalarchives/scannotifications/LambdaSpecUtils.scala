package uk.gov.nationalarchives.scannotifications

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{ok, post, urlEqualTo}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import uk.gov.nationalarchives.scannotifications.decoders.SSMMaintenanceDecoder.SSMMaintenanceEvent
import uk.gov.nationalarchives.scannotifications.decoders.ScanDecoder.{ScanDetail, ScanEvent, ScanFindingCounts}

class LambdaSpecUtils extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with TableDrivenPropertyChecks {

  def getCounts(scanEvent: ScanEvent): (Int, Int, Int, Int) = {
    val critical = scanEvent.detail.findingSeverityCounts.critical.getOrElse(0)
    val high = scanEvent.detail.findingSeverityCounts.high.getOrElse(0)
    val medium = scanEvent.detail.findingSeverityCounts.medium.getOrElse(0)
    val low = scanEvent.detail.findingSeverityCounts.low.getOrElse(0)
    (critical, high, medium, low)
  }

  val scanEvent1: ScanEvent = ScanEvent(ScanDetail("", List("latest"), ScanFindingCounts(Some(10), Some(100), Some(1000), Some(10000))))
  val scanEvent2: ScanEvent = ScanEvent(ScanDetail("", List("latest"), ScanFindingCounts(Option.empty, Some(0), Option.empty, Some(10))))
  val scanEvent3: ScanEvent = ScanEvent(ScanDetail("", List("latest"), ScanFindingCounts(Option.empty, Option.empty, Option.empty, Option.empty)))
  val scanEvent4: ScanEvent = ScanEvent(ScanDetail("", List("anothertag"), ScanFindingCounts(Option.empty, Option.empty, Option.empty, Option.empty)))
  val scanEvent5: ScanEvent = ScanEvent(ScanDetail("", List("latest"), ScanFindingCounts(Some(0), Some(0), Some(0), Some(0))))
  val maintenanceResult1: SSMMaintenanceEvent = SSMMaintenanceEvent(true)
  val maintenanceResult2: SSMMaintenanceEvent = SSMMaintenanceEvent(false)

  val events: TableFor3[String, List[String], List[String]] =
    Table(
      ("input", "emailBody", "slackBody"),
      (scanEventInputText(scanEvent1), scanEventBody(scanEvent1), scanEventBodyJson(scanEvent1)),
      (scanEventInputText(scanEvent2), scanEventBody(scanEvent2), scanEventBodyJson(scanEvent2)),
      (scanEventInputText(scanEvent3), List(), List()),
      (scanEventInputText(scanEvent4), List(), List()),
      (maintenanceEventInputText(maintenanceResult1), List(), List()),
      (maintenanceEventInputText(maintenanceResult2), List(), maintenanceEventBodyJson)
    )

  val wiremockSesEndpoint = new WireMockServer(9001)
  val wiremockSlackServer = new WireMockServer(9002)

  override def beforeEach(): Unit = {
    wiremockSlackServer.stubFor(post(urlEqualTo("/webhook")).willReturn(ok("")))
    wiremockSesEndpoint.stubFor(post(urlEqualTo("/"))
      .willReturn(ok(
        """
          |<SendEmailResponse xmlns="https://email.amazonaws.com/doc/2010-03-31/">
          |  <SendEmailResult>
          |    <MessageId>000001271b15238a-fd3ae762-2563-11df-8cd4-6d4e828a9ae8-000000</MessageId>
          |  </SendEmailResult>
          |  <ResponseMetadata>
          |    <RequestId>fd3ae762-2563-11df-8cd4-6d4e828a9ae8</RequestId>
          |  </ResponseMetadata>
          |</SendEmailResponse>
          |""".stripMargin)))
  }

  override def afterEach(): Unit = {
    wiremockSlackServer.resetAll()
    wiremockSesEndpoint.resetAll()
  }

  override def beforeAll(): Unit = {
    wiremockSlackServer.start()
    wiremockSesEndpoint.start()
  }

  override def afterAll(): Unit = {
    wiremockSlackServer.stop()
    wiremockSesEndpoint.stop()
  }

  def scanEventBody(scanEvent: ScanEvent): List[String] = {
    val (critical, high, medium, low) = getCounts(scanEvent)
    List("Action=SendEmail&Version=2010-12-01&Source=scanresults%40tdr-management.nationalarchives.gov.uk" +
      "&Destination.ToAddresses.member.1=aws_tdr_management%40nationalarchives.gov.uk" +
      "&Message.Subject.Data=ECR+scan+results+for+yara-dependencies&Message.Subject.Charset=UTF-8" +
      "&Message.Body.Html.Data=%3Chtml%3E%3Cbody%3E%3Ch1%3EImage+scan+results+for+yara-dependencies%3C%2Fh1%3E%3Cdiv%3E%3Cp%3E" +
      s"$critical+critical+vulnerabilities%3C%2Fp%3E%3Cp%3E" +
      s"$high+high+vulnerabilities%3C%2Fp%3E%3Cp%3E" +
      s"$medium+medium+vulnerabilities%3C%2Fp%3E%3Cp%3E" +
      s"$low+low+vulnerabilities%3C%2Fp%3E%3C%2Fdiv%3E%3C%2Fbody%3E%3C%2Fhtml%3E" +
      "&Message.Body.Html.Charset=UTF-8")
  }

  def scanEventBodyJson(scanEvent: ScanEvent): List[String] = {
    val (critical, high, medium, low) = getCounts(scanEvent)
    List(s"""
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
       |  } ]
       |}
       |""".stripMargin)
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

  def maintenanceEventInputText(ssmMaintenanceResult: SSMMaintenanceEvent): String = {
    val status = if(ssmMaintenanceResult.success) "SUCCESS" else "FAILED"
    s"""
       |{
       |  "detail": {
       |    "status": "$status"
       |  }
       |}
       |""".stripMargin
  }

  def maintenanceEventBodyJson: List[String] = {
    List(s"""{
       |  "blocks" : [ {
       |    "type" : "section",
       |    "text" : {
       |      "type" : "mrkdwn",
       |      "text" : "The Jenkins backup has failed. Please check the maintenance window in systems manager"
       |    }
       |  } ]
       |}""".stripMargin)
  }
}
