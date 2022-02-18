import sbt._

object Dependencies {
  private val log4CatsVersion = "1.1.1"
  private val githubPureConfigVersion = "0.14.0"

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "3.2.9"
  lazy val sttp = "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.3.15"
  lazy val sttpCirce = "com.softwaremill.sttp.client3" %% "circe" % "3.3.15"
  lazy val log4cats = "io.chrisdavenport" %% "log4cats-core" % log4CatsVersion
  lazy val log4catsSlf4j = "io.chrisdavenport" %% "log4cats-slf4j" % log4CatsVersion
  lazy val slf4j = "org.slf4j" % "slf4j-simple" % "1.7.30"
  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % githubPureConfigVersion
  lazy val pureConfigCatsEffect = "com.github.pureconfig" %% "pureconfig-cats-effect" % githubPureConfigVersion
  lazy val circeCore = "io.circe" %% "circe-core" % "0.13.0"
  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.13.0"
  lazy val circeParser = "io.circe" %% "circe-parser" % "0.13.0"
  lazy val typesafe = "com.typesafe" % "config" % "1.4.0"
  lazy val scalaTags = "com.lihaoyi" %% "scalatags" % "0.8.2"
  lazy val awsUtils =  "uk.gov.nationalarchives.aws.utils" %% "tdr-aws-utils" % "0.1.20"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock" % "2.27.2"
}
