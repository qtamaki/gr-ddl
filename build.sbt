//import AssemblyKeys._

name := "gr-ddl"

organization := "jp.co.applicative.tool"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" % "scala-swing_2.11" % "2.0.0-M2",
  "org.specs2" %% "specs2-core" % "3.8.4" % "test",
  "org.apache.poi" % "poi" % "3.14"
)

initialCommands := "import jp.co.applicative.tool.ddl._"

//assemblySettings

//assemblyJarName in assembly := "gr-ddl.jar"

mainClass in assembly := Some("jp.co.applicative.tool.ddl.App")

