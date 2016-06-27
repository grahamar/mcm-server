
lazy val `mcm-server` = project.in(file("."))
  .enablePlugins(JavaAppPackaging, JettyAlpn)

name := "mcm-server"

organization := "io.grhodes"

version := "git describe --tags --dirty --always".!!.stripPrefix("v").trim

scalaVersion := "2.11.8"

lazy val circeVersion = "0.4.1"
lazy val http4sVersion = "0.14.1"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.0" % Optional,
  "ch.qos.logback" % "logback-classic" % "1.1.7" % Optional,
  "com.google.guava" % "guava" % "19.0",
  "com.gilt" %% "gfc-logging" % "0.0.5",
  "com.gilt" %% "gfc-util" % "0.1.3",
  "org.apache.vysper" % "vysper-core" % "0.7",
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion
)

alpnApiIncluded := true
alpnApiVersion := "1.1.2.v20150522"

fork in run := true
fork in runMain := true
javaOptions in Runtime ++= Seq("-Djavax.net.debug=ssl,handshake")
