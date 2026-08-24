package uk.gov.nationalarchives.notifications

import io.circe.parser.decode
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.notifications.decoders.EcsDeploymentStateChangeDecoder.{EcsDeploymentDetail, EcsDeploymentStateChangeEvent, EcsTaskContainer}
import uk.gov.nationalarchives.notifications.decoders.IncomingEvent

class EcsDeploymentStateChangeDecoderSpec extends AnyFlatSpec with Matchers {
  "IncomingEvent decoder" should "decode an SNS wrapped ECS task state change event" in {
    val input =
      """
        |{
        |  "Records": [
        |    {
        |      "Sns": {
        |        "Message": "{\"version\":\"0\",\"id\":\"b1c2d3e4-0000-1111-2222-333344445555\",\"detail-type\":\"ECS Task State Change\",\"source\":\"aws.ecs\",\"account\":\"123456789012\",\"time\":\"2026-08-24T10:16:00Z\",\"region\":\"eu-west-2\",\"resources\":[\"arn:aws:ecs:eu-west-2:123456789012:task/frontend_prod/1234567890abcdef\"],\"detail\":{\"clusterArn\":\"arn:aws:ecs:eu-west-2:123456789012:cluster/frontend_prod\",\"taskArn\":\"arn:aws:ecs:eu-west-2:123456789012:task/frontend_prod/1234567890abcdef\",\"lastStatus\":\"STOPPED\",\"stopCode\":\"EssentialContainerExited\",\"stoppedReason\":\"Essential container in task exited\",\"containers\":[{\"name\":\"frontend\",\"image\":\"image\",\"exitCode\":255,\"reason\":\"OutOfMemoryError\"}]}}"
        |      }
        |    }
        |  ]
        |}
        |""".stripMargin

    val expected = EcsDeploymentStateChangeEvent(
      `detail-type` = "ECS Task State Change",
      detail = EcsDeploymentDetail(
        taskArn = "arn:aws:ecs:eu-west-2:123456789012:task/frontend_prod/1234567890abcdef",
        containers = List(EcsTaskContainer("frontend", "image" , 255, "OutOfMemoryError"))
      )
    )

    decode[IncomingEvent](input) shouldBe Right(expected)
  }
}
