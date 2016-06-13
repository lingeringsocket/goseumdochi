name := "goseumdochi-root"

val javacppVersion = Common.javacppVersion

val javacppPointVersion = Common.javacppPointVersion

val opencvVersion = Common.opencvVersion

val ffmpegVersion = Common.ffmpegVersion

organization := Common.organization

version := Common.version

isSnapshot := true

scalaVersion := Common.scalaVersion

scalacOptions := Common.scalacOptions

lazy val platform = org.bytedeco.javacpp.Loader.getPlatform

classpathTypes += "maven-plugin"

autoCompilerPlugins := true

parallelExecution in Test := false

fork := true

javaOptions += "-Xmx1G"

lazy val base = project

lazy val `desktop-sphero` = project.
  dependsOn(base % "compile->compile;test->test")

lazy val root = (project in file(".")).
  aggregate(base, `desktop-sphero`).
  dependsOn(`desktop-sphero`)

lazy val android = project.dependsOn(base)

mainClass in Compile := Some("org.goseumdochi.ConsoleMain")

resolvers ++= Common.resolvers

libraryDependencies ++= Seq(
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) classifier platform,
  "org.bytedeco.javacpp-presets" % "ffmpeg" % (ffmpegVersion + "-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "ffmpeg" % (ffmpegVersion + "-" + javacppVersion) classifier platform,
  "org.scalafx" %% "scalafx" % "8.0.60-R9"
)

libraryDependencies ++= Common.javacvDeps

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.ivy2/local/org.goseumdochi")))
