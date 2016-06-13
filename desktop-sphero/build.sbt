name := "goseumdochi-desktop-sphero"

organization := Common.organization

version := Common.version

isSnapshot := true

scalaVersion := Common.scalaVersion

val javacppVersion = Common.javacppVersion

val javacppPointVersion = Common.javacppPointVersion

val opencvVersion = Common.opencvVersion

scalacOptions ++= Common.scalacOptions

scalacOptions in Test ++= Seq("-Yrangepos")

lazy val platform = org.bytedeco.javacpp.Loader.getPlatform

classpathTypes += "maven-plugin"

resolvers ++= Common.resolvers

libraryDependencies ++= Seq(
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) classifier platform
)

libraryDependencies ++= Common.javacvDeps

libraryDependencies ++= Common.specs2Deps

libraryDependencies ++= Common.akkaDeps

autoCompilerPlugins := true

parallelExecution in Test := false

fork := true

javaOptions += "-Xmx1G"

maxErrors := 99

traceLevel := 10

scalastyleFailOnError := true

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.ivy2/local/org.goseumdochi")))
