name := "goseumdochi-desktop-sphero"

organization := "org.goseumdochi"

version := "0.1"

isSnapshot := true

scalaVersion := "2.11.7"

val javacppVersion = "1.1"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint",
  "-Xfatal-warnings", "-Ywarn-unused-import")

scalacOptions in Test ++= Seq("-Yrangepos")

lazy val platform = org.bytedeco.javacpp.Loader.getPlatform

classpathTypes += "maven-plugin"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "com.typesafe.akka"      %% "akka-actor"     % "2.3.15",
  "com.typesafe.akka"      %% "akka-testkit"     % "2.3.15" % "test",
  "org.bytedeco"                 % "javacpp"         % javacppVersion,
  "org.bytedeco"                 % "javacv"          % javacppVersion,
  "org.bytedeco.javacpp-presets" % "opencv" % ("3.0.0-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % ("3.0.0-" + javacppVersion) classifier platform,
  "org.specs2"        %% "specs2-core"             % "3.7.2"           % "test"
)

autoCompilerPlugins := true

parallelExecution in Test := false

fork := true

javaOptions += "-Xmx1G"

maxErrors := 99

traceLevel := 10

scalastyleFailOnError := true

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.ivy2/local/org.goseumdochi")))
