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

  val diskSpaceFullMessage =
    s"""
       |{
       |    "Records": [
       |        {
       |            "Sns": {
       |                "Message": "{\\"AlarmName\\":\\"tdr-jenkins-disk-space-alarm-mgmt\\",\\"NewStateValue\\":\\"OK\\",\\"Trigger\\":{\\"Dimensions\\":[{\\"value\\":\\"Jenkins\\",\\"name\\":\\"server_name\\"}],\\"Threshold\\":20.0}}"
       |            }
       |        }
       |    ]
       |}
       |
       |""".stripMargin

  val transformEngineRetryMessage =
    s"""
       |{
       |  "Records": [
       |        {
       |            "body": "{\\"consignment-reference\\": \\"some-consignment-reference\\",\\"consignment-type\\": \\"judgment\\",\\"number-of-retries\\": 0}"
       |        }
       |  ]
       |}
       |""".stripMargin

  val inputStream = new ByteArrayInputStream(ecrScanMessage.getBytes)

  // The Lambda does not use the output stream, so it's safe to set it to null
  val outputStream = null

  lambda.process(inputStream, outputStream)

  System.exit(0)
}
