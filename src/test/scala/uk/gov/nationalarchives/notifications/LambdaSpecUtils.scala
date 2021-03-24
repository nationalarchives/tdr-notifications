package uk.gov.nationalarchives.notifications

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, ok, okJson, post, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class LambdaSpecUtils extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  val wiremockSesEndpoint = new WireMockServer(9001)
  val wiremockSlackServer = new WireMockServer(9002)
  val wiremockKmsEndpoint = new WireMockServer(9004)

  def stubKmsResponse(cipherText: String): StubMapping =
    wiremockKmsEndpoint.stubFor(post(urlEqualTo("/"))
      .withRequestBody(equalToJson(s"""{"CiphertextBlob":"$cipherText","EncryptionContext":{"LambdaFunctionName":"test-lambda-function"}}"""))
      .willReturn(okJson(s"""{"Plaintext": "$cipherText"}"""))
    )

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

    stubKmsResponse("aHR0cDovL2xvY2FsaG9zdDo5MDAyL3dlYmhvb2s=")
    stubKmsResponse("YXdzX3Rkcl9tYW5hZ2VtZW50QG5hdGlvbmFsYXJjaGl2ZXMuZ292LnVr")
    stubKmsResponse("Q1ZFLTIwMTYtMTIzNDU2NzgsQ1ZFLTIwMjAtOTg3NjU=")


    super.beforeEach()
  }

  override def afterEach(): Unit = {
    wiremockSlackServer.resetAll()
    wiremockSesEndpoint.resetAll()
    wiremockKmsEndpoint.resetAll()

    super.afterEach()
  }

  override def beforeAll(): Unit = {
    wiremockSlackServer.start()
    wiremockSesEndpoint.start()
    wiremockKmsEndpoint.start()

    super.beforeAll()
  }

  override def afterAll(): Unit = {
    wiremockSlackServer.stop()
    wiremockSesEndpoint.stop()
    wiremockKmsEndpoint.stop()

    super.afterAll()
  }
}
