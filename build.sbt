import AssemblyKeys._

name := "gr-ddl"

organization := "jp.co.applicative.tool"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-swing" % "2.10.2",
  "org.specs2" %% "specs2" % "1.13" % "test",
  "org.apache.poi" % "poi" % "3.9"
)

initialCommands := "import jp.co.applicative.tool.ddl._"

assemblySettings