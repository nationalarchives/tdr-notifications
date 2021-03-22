package uk.gov.nationalarchives.notifications

import java.io.ByteArrayInputStream

// An entry point with which to run the Lambda locally
object LambdaRunner extends App {
  val lambda = new Lambda

  val message =
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
  val inputStream = new ByteArrayInputStream(message.getBytes)

  // The Lambda does not use the output stream, so it's safe to set it to null
  val outputStream = null

  lambda.process(inputStream, outputStream)

  System.exit(0)
}
