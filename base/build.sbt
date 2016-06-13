name := "goseumdochi-base"

organization := Common.organization

version := Common.version

isSnapshot := true

exportJars := true

scalaVersion := Common.scalaVersion

val javacppVersion = Common.javacppVersion

val javacppPointVersion = Common.javacppPointVersion

val opencvVersion = Common.opencvVersion

val ffmpegVersion = Common.ffmpegVersion

scalacOptions ++= Common.scalacOptions

scalacOptions in Test ++= Seq("-Yrangepos")

lazy val platform = org.bytedeco.javacpp.Loader.getPlatform

classpathTypes += "maven-plugin"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

resolvers += "bintray/meetup" at "http://dl.bintray.com/meetup/maven"

resolvers ++= Common.resolvers

libraryDependencies ++= Seq(
  "com.meetup" %% "archery" % "0.4.0",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) % "test" classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) % "test" classifier platform,
  "org.bytedeco.javacpp-presets" % "ffmpeg" % (ffmpegVersion + "-" + javacppVersion) % "test" classifier "",
  "org.bytedeco.javacpp-presets" % "ffmpeg" % (ffmpegVersion + "-" + javacppVersion) % "test" classifier platform,
  "com.owlike" %% "genson-scala" % "1.4",
  "com.jsuereth" %% "scala-arm" % "1.4"
)

libraryDependencies ++= Common.javacvDeps

libraryDependencies ++= Common.specs2Deps

libraryDependencies ++= Common.akkaDeps

autoCompilerPlugins := true

parallelExecution in Test := false

fork := true

// make Android happy
javacOptions ++= Seq("-target", "1.7", "-source", "1.7")

javaOptions += "-Xmx1G"

maxErrors := 99

traceLevel := 10

scalastyleFailOnError := true

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.ivy2/local/org.goseumdochi")))
