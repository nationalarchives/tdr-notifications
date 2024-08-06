package uk.gov.nationalarchives.notifications

import java.io.ByteArrayInputStream

// An entry point with which to run the Lambda locally
object LambdaRunner extends App {
  val lambda = new Lambda

//  There are three 'message' variables so that different types of Slack notification messages can be tested locally
  val ecrScanMessage =
    s"""
       |{
       |  "detail": {
       |    "scan-status": "COMPLETE",
       |    "repository-name": "yara",
       |    "image-tags" : ["intg"],
       |    "image-digest": "sha256:401d492b2ab0d6639a3889cafae28188666c1ebd827d86e0ce67819211a0c062",
       |    "finding-severity-counts": {
       |      "CRITICAL": 0,
       |      "HIGH": 0,
       |      "MEDIUM": 0,
       |      "LOW": 1
       |    }
       |  }
       |}
       |""".stripMargin

    val exportSuccessMessage = s"""
       |{
       |    "Records": [
       |        {
       |            "Sns": {
       |                "Message": "{\\"success\\":true,\\"consignmentId\\":\\"fc2b60ba-8078-4e7a-b91b-a5b6c616a9bc\\", \\"environment\\": \\"some-environment\\", \\"successDetails\\":{\\"userId\\": \\"fc2b60ba-8078-4e7a-b91b-a5b6c616a9bc\\",\\"consignmentReference\\": \\"some-consignment-reference\\",\\"transferringBodyName\\": \\"some-transferring-body-code\\",\\"consignmentType\\": \\"judgment\\", \\"exportBucket\\": \\"some-export-bucket\\"}}"
       |            }
       |        }
       |    ]
       |}
       |
       |""".stripMargin

    val exportFailureMessage =
      s"""
         |{
         |    "Records": [
         |        {
         |            "Sns": {
         |                "Message": "{\\"success\\":false,\\"consignmentId\\":\\"fc2b60ba-8078-4e7a-b91b-a5b6c616a9bc\\",\\"environment\\": \\"some-environment\\",\\"failureCause\\":\\"Cause of failure\\"}"
         |            }
         |        }
         |    ]
         |}
         |
         |""".stripMargin

  val cloudwatchAlarmMessage =
    s"""
       |{
       |    "Records": [
       |        {
       |            "Sns": {
       |                "Message": "{\\"AlarmName\\":\\"DDoSDetectedAlarmForProtection\\",\\"NewStateValue\\":\\"ALARM\\",\\"NewStateReason\\":\\"Test Reason\\", \\"Trigger\\":{\\"MetricName\\": \\"TestName\\", \\"Dimensions\\":[{\\"value\\":\\"test-resource-arn\\",\\"name\\":\\"ResourceArn\\"}]}}"
       |            }
       |        }
       |    ]
       |}
       |
       |""".stripMargin

    val notifyRotationMessage =
    s"""
       |{
       |    "Records": [
       |        {
       |            "Sns": {
       |                "Message": "{\\"detail\\":{\\"parameter-name\\":\\"/some/ssm/parameter\\",\\"action-reason\\":\\"The parameter has not been changed for 1 hour. This notification was generated based on the policy created at 2022-10-05T06:02:08.334338Z.\\"}}"
       |            }
       |        }
       |    ]
       |}
       |""".stripMargin

  val genericMessage =
    s"""
       |{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"messages\\":[{\\"message\\":\\"A test message\\"}]}"
       |      }
       |    }
       |  ]}
       |""".stripMargin

  val metadataRequest =
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"transferringBodyName\\":\\"transferringBodyTest\\",\\"consignmentReference\\":\\"consignmentReferenceTest\\",\\"consignmentId\\" : \\"consignmentIdTest\\",\\"userId\\" : \\"userIdTest\\",\\"userEmail\\" : \\"thanh.lam@nationalarchives.gov.uk\\"}"
       |      }
       |    }
       |  ]}""".stripMargin

  val metadataSubmitted =
    s"""{
       | "Records": [
       |   {
       |     "Sns": {
       |       "Message": "{\\"consignmentReference\\":\\"consignmentReferenceTest\\",\\"urlLink\\" : \\"https://www.nationalarchives.gov.uk/\\",\\"userEmail\\" : \\"thanh.lam@nationalarchives.gov.uk\\",\\"status\\" : \\"Completed\\"}"
       |      }
       |    }
       |  ]}""".stripMargin

  val inputStream = new ByteArrayInputStream(cloudwatchAlarmMessage.getBytes)

  // The Lambda does not use the output stream, so it's safe to set it to null
  val outputStream = null

  lambda.process(inputStream, outputStream)

  System.exit(0)
}
