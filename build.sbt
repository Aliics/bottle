ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.2.2"

lazy val root = (project in file("."))
  .aggregate(service, publisher, subscriber)

lazy val service = (project in file("service"))
  .settings(
    name := "bottle-service",
    moduleName := "Bottle service",
    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % Versions.scalaLogging,
      "org.slf4j" % "slf4j-simple" % Versions.slf4j,
      "com.lihaoyi" %% "upickle" % Versions.upickle,
    ),
  )

lazy val publisher = (project in file("publisher"))
  .settings(
    name := "bottle-publisher",
    moduleName := "Bottle publisher client",
  )

lazy val subscriber = (project in file("subscriber"))
  .settings(
    name := "bottle-subscriber",
    moduleName := "Bottle subscriber client",
  )
