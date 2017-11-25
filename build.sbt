name := "dataroot-todo-test"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.1.4",
  "com.typesafe.slick" %% "slick" % "3.2.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "com.github.scopt" %% "scopt" % "3.7.0"
)