import Dependencies._

ThisBuild / scalaVersion := "2.13.17"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "notifications",
    libraryDependencies ++= Seq(
      circeCore,
      circeGeneric,
      circeParser,
      sttp,
      sttpCirce,
      kmsUtils,
      sesUtils,
      snsUtils,
      ecrUtils,
      ssmUtils,
      typesafe,
      scalaTags,
      typeSafeLogging,
      transformSchemas,
      notifyJavaClient,
      scalaTest % Test,
      wiremock % Test,
      elasticMq % Test
    )
  )

(Test / fork) := true
(Test / javaOptions) += s"-Dconfig.file=${sourceDirectory.value}/test/resources/application.conf"
(Test / envVars) := Map("AWS_ACCESS_KEY_ID" -> "test", "AWS_SECRET_ACCESS_KEY" -> "test", "AWS_SESSION_TOKEN" -> "test")

resolvers += "TDR Releases" at "s3://tdr-releases-mgmt"
(assembly / assemblyJarName) := "notifications.jar"

(assembly / assemblyMergeStrategy) := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _                             => MergeStrategy.first
}
