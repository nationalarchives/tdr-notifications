package uk.gov.nationalarchives.notifications

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{ok, post, urlEqualTo}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class LambdaSpecUtils extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

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

    super.beforeEach()
  }

  override def afterEach(): Unit = {
    wiremockSlackServer.resetAll()
    wiremockSesEndpoint.resetAll()

    super.afterEach()
  }

  override def beforeAll(): Unit = {
    wiremockSlackServer.start()
    wiremockSesEndpoint.start()

    super.beforeAll()
  }

  override def afterAll(): Unit = {
    wiremockSlackServer.stop()
    wiremockSesEndpoint.stop()

    super.afterAll()
  }
}
