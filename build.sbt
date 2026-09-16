import Dependencies._

ThisBuild / scalaVersion := "2.13.18"
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
dependencyOverrides ++= Seq(
  "io.netty" % "netty-buffer" % "4.1.137.Final",
  "io.netty" % "netty-codec" % "4.1.137.Final",
  "io.netty" % "netty-codec-http" % "4.1.137.Final",
  "io.netty" % "netty-codec-http2" % "4.1.137.Final",
  "io.netty" % "netty-codec-socks" % "4.1.137.Final",
  "io.netty" % "netty-common" % "4.1.137.Final",
  "io.netty" % "netty-handler" % "4.1.137.Final",
  "io.netty" % "netty-handler-proxy" % "4.1.137.Final",
  "io.netty" % "netty-resolver" % "4.1.137.Final",
  "io.netty" % "netty-transport" % "4.1.137.Final",
  "io.netty" % "netty-transport-classes-epoll" % "4.1.137.Final",
  "io.netty" % "netty-transport-native-unix-common" % "4.1.137.Final"
)

resolvers += "TDR Releases" at "s3://tdr-releases-mgmt"
(assembly / assemblyJarName) := "notifications.jar"

(assembly / assemblyMergeStrategy) := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _                             => MergeStrategy.first
}
