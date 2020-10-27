package uk.gov.nationalarchives.scannotifications

import java.nio.charset.StandardCharsets
import java.util.Base64

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.nationalarchives.scannotifications.decoders.ScanDecoder.{ScanDetail, ScanEvent, ScanFindingCounts}

class LambdaSpec extends LambdaSpecUtils {

  forAll(events) {
    (input, emailBody, slackBody) => {
      "the process method" should s"send an email message for event $input" in {
        val expectedBody = Base64.getEncoder.encodeToString(emailBody.headOption.getOrElse("").getBytes(StandardCharsets.UTF_8))
        val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
        new Lambda().process(stream, null)
        wiremockSesEndpoint.verify(emailBody.size,
          postRequestedFor(urlEqualTo("/"))
            .withRequestBody(binaryEqualTo(expectedBody))
        )
      }

      "the process method" should s"send a slack message for event $input" in {
        val stream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
        new Lambda().process(stream, null)
        wiremockSlackServer.verify(slackBody.size,
          postRequestedFor(urlEqualTo("/webhook"))
            .withRequestBody(equalToJson(slackBody.headOption.getOrElse("{}")))
        )
      }
    }
  }

  "the process method" should "error if the ses service is unavailable" in {
    val scanEvent = ScanEvent(ScanDetail("", List("latest"), ScanFindingCounts(Some(10), Some(100), Some(1000), Some(10000))))
    val stream = new java.io.ByteArrayInputStream(scanEventInputText(scanEvent).getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
    wiremockSesEndpoint.resetAll()
    val exception = intercept[Exception] {
      new Lambda().process(stream, null)
    }
    exception.getMessage should be("null (Service: Ses, Status Code: 404, Request ID: null, Extended Request ID: null)")
  }

  "the process method" should "error if the slack service is unavailable" in {
    val scanEvent = ScanEvent(ScanDetail("", List("latest"), ScanFindingCounts(Some(10), Some(100), Some(1000), Some(10000))))
    val stream = new java.io.ByteArrayInputStream(scanEventInputText(scanEvent).getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
    wiremockSlackServer.resetAll()
    val exception = intercept[Exception] {
      new Lambda().process(stream, null)
    }
    exception.getMessage should be("No response could be served as there are no stub mappings in this WireMock instance.")
  }
}
