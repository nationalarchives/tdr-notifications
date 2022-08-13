import sbt._

object Dependencies {
  private val circeVersion = "0.14.2"
  private val sttpClient3Version = "3.7.4"
  private val elasticMqVersion = "1.3.9"

  lazy val sttp = "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % sttpClient3Version
  lazy val sttpCirce = "com.softwaremill.sttp.client3" %% "circe" % sttpClient3Version
  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  lazy val typesafe = "com.typesafe" % "config" % "1.4.2"
  lazy val scalaTags = "com.lihaoyi" %% "scalatags" % "0.11.1"
  lazy val awsUtils =  "uk.gov.nationalarchives" %% "tdr-aws-utils" % "0.1.35"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.13"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock" % "2.27.2"
  lazy val typesafeConfig = "com.typesafe" % "config" % "1.4.1"
  lazy val typeSafeLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
  lazy val elasticMq = "org.elasticmq" %% "elasticmq-server" % elasticMqVersion
  lazy val elasticMqSqs = "org.elasticmq" %% "elasticmq-rest-sqs" % elasticMqVersion
}
