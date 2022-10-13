package uk.gov.nationalarchives.notifications

import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{ok, post, urlEqualTo}
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.{Parameters, ResponseDefinitionTransformer}
import com.github.tomakehurst.wiremock.http.{Request, ResponseDefinition}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import io.circe.generic.auto._
import io.circe.parser.decode
import org.elasticmq.rest.sqs.{SQSRestServer, SQSRestServerBuilder}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model._

import scala.jdk.CollectionConverters._

class LambdaSpecUtils extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  val wiremockSesEndpoint = new WireMockServer(9001)
  val wiremockSlackServer = new WireMockServer(9002)
  val wiremockKmsEndpoint = new WireMockServer(new WireMockConfiguration().port(9004).extensions(new ResponseDefinitionTransformer {
    override def transform(request: Request, responseDefinition: ResponseDefinition, files: FileSource, parameters: Parameters): ResponseDefinition = {
      case class KMSRequest(CiphertextBlob: String)
      decode[KMSRequest](request.getBodyAsString) match {
        case Left(err) => throw err
        case Right(req) =>
          val charset = Charset.defaultCharset()
          val plainText = charset.newDecoder.decode(ByteBuffer.wrap(req.CiphertextBlob.getBytes(charset))).toString
          ResponseDefinitionBuilder
            .like(responseDefinition)
            .withBody(s"""{"Plaintext": "$plainText"}""")
            .build()
      }
    }
    override def getName: String = ""
  }))

  def stubKmsResponse: StubMapping = wiremockKmsEndpoint.stubFor(post(urlEqualTo("/")))

  override def beforeEach(): Unit = {
    wiremockSlackServer.stubFor(post(urlEqualTo("/webhook")).willReturn(ok("")))
    wiremockSlackServer.stubFor(post(urlEqualTo("/webhook-judgment")).willReturn(ok("")))
    wiremockSlackServer.stubFor(post(urlEqualTo("/webhook-tdr")).willReturn(ok("")))
    wiremockSlackServer.stubFor(post(urlEqualTo("/webhook-export")).willReturn(ok("")))
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

    stubKmsResponse
    transformEngineQueueHelper.createQueue

    super.beforeEach()
  }

  override def afterEach(): Unit = {
    wiremockSlackServer.resetAll()
    wiremockSesEndpoint.resetAll()
    wiremockKmsEndpoint.resetAll()
    transformEngineQueueHelper.deleteQueue()

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

  case class QueueHelper(queueUrl: String) {
    val sqsClient: SqsClient = SqsClient.builder()
      .region(Region.EU_WEST_2)
      .endpointOverride(URI.create("http://localhost:8002"))
      .build()

    def send(body: String): SendMessageResponse = sqsClient.sendMessage(SendMessageRequest
      .builder.messageBody(body).queueUrl(queueUrl).build())

    def receive: List[Message] = sqsClient.receiveMessage(ReceiveMessageRequest
      .builder
      .maxNumberOfMessages(10)
      .queueUrl(queueUrl)
      .build).messages.asScala.toList

    val visibilityTimeoutAttributes = new util.HashMap[QueueAttributeName, String]()
    visibilityTimeoutAttributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, (12 * 60 * 60).toString)

    def createQueue: CreateQueueResponse = sqsClient.createQueue(
      CreateQueueRequest.builder.queueName(queueUrl.split("/")(4)).attributes(visibilityTimeoutAttributes).build()
    )
    def deleteQueue(): DeleteQueueResponse = sqsClient.deleteQueue(DeleteQueueRequest.builder.queueUrl(queueUrl).build())
  }

  case class TopicHelper(topic: String) {

    def receive(): Unit = {}

  }

  val port = 8002
  val transformEngineQueueName = "transform_engine_sqs_queue"
  val sqsApi: SQSRestServer = SQSRestServerBuilder.withPort(port).withAWSRegion(Region.EU_WEST_2.toString).start()

  val transformEngineQueue = s"http://localhost:$port/queue/$transformEngineQueueName"
  val transformEngineQueueHelper: QueueHelper = QueueHelper(transformEngineQueue)
  val transformEngineTopicHelper: TopicHelper = TopicHelper("something")
}
