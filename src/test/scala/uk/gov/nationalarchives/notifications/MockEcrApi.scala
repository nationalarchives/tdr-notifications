package uk.gov.nationalarchives.notifications

import com.github.tomakehurst.wiremock.WireMockServer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

trait MockEcrApi extends AnyFlatSpec with BeforeAndAfterAll with BeforeAndAfterEach {
  val ecrApiEndpoint = new WireMockServer(9003)

  override def afterEach(): Unit = {
    ecrApiEndpoint.resetAll()
    super.afterEach()
  }

  override def beforeAll(): Unit = {
    ecrApiEndpoint.start()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    ecrApiEndpoint.stop()
    super.afterAll()
  }
}
