import AssemblyKeys._

name := "BaseProject"

organization := "com.mpp"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.13" % "test"
)

initialCommands := "import com.mpp.baseproject._"

assemblySettings