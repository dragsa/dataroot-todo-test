name := "dataroot-todo-test"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.1.4",
  "com.typesafe.slick" %% "slick" % "3.2.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "com.github.scopt" %% "scopt" % "3.7.0",
  "com.typesafe.akka" %% "akka-actor" % "2.5.6",
  "org.eu.acolyte" % "jdbc-scala_2.11" % "1.0.47" % "test",
  "org.specs2" %% "specs2-core" % "4.0.1" % "test"
)