package uk.gov.nationalarchives.notifications

import uk.gov.nationalarchives.notifications.EcrScanIntegrationSpec.scanEventInputText
import uk.gov.nationalarchives.notifications.decoders.ScanDecoder.{ScanDetail, ScanEvent, ScanFindingCounts}

class LambdaErrorSpec extends LambdaSpecUtils {

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
