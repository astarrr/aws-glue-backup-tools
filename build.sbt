ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "glue-backup-tool",
    libraryDependencies ++= Seq(
      "com.monovore" %% "decline" % "2.5.0",
      "software.amazon.awssdk" % "glue" % "2.31.1",
      "org.scala-lang" %% "toolkit" % "0.7.0",
      "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.43.0" % "test",
      "com.dimafeng" %% "testcontainers-scala-localstack" % "0.43.0" % "test",
      "org.scalatest" %% "scalatest" % "3.2.19" % "test"
    )
  )

enablePlugins(JavaAppPackaging)