name := "goseumdochi"

organization := "org.goseumdochi"

val javacppVersion = "0.11"

version := "0.1"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint",
  "-Xfatal-warnings", "-Ywarn-unused-import")

scalacOptions in Test ++= Seq("-Yrangepos")

lazy val platform = org.bytedeco.javacpp.Loader.getPlatform

classpathTypes += "maven-plugin"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "com.jsuereth" %% "scala-arm" % "1.4",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "com.typesafe.akka"      %% "akka-actor"     % "2.4.1",
  "com.typesafe.akka"      %% "akka-testkit"     % "2.4.1" % "test",
  "org.bytedeco"                 % "javacpp"         % javacppVersion,
  "org.bytedeco"                 % "javacv"          % javacppVersion,
  "org.bytedeco.javacpp-presets" % "opencv" % ("2.4.11-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % ("2.4.11-" + javacppVersion) classifier platform,
  "org.scalafx" %% "scalafx" % "8.0.60-R9",
  "org.specs2"        %% "specs2-core"             % "3.7.2"           % "test"
)

autoCompilerPlugins := true

parallelExecution in Test := false

fork := true

javaOptions += "-Xmx1G"

maxErrors := 99

traceLevel := 10

scalastyleFailOnError := true

mainClass in Compile := Some("org.goseumdochi.ConsoleMain")
