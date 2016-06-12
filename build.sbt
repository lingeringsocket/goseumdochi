name := "goseumdochi-root"

val javacppVersion = "1.2"

val opencvVersion = "3.1.0"

val ffmpegVersion = "3.0.2"

version := "0.1"

isSnapshot := true

scalaVersion := "2.11.7"

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

resolvers ++= Seq(Resolver.mavenLocal,
  DefaultMavenRepository,
  Resolver.typesafeRepo("releases"),
  Resolver.typesafeRepo("snapshots"),
  Resolver.typesafeIvyRepo("snapshots"),
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  Resolver.defaultLocal,
  bintray.Opts.resolver.jcenter)

libraryDependencies ++= Seq(
  "org.bytedeco"                 % "javacpp"         % "1.2.1",
  "org.bytedeco"                 % "javacv"          % javacppVersion,
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) classifier platform,
  "org.bytedeco.javacpp-presets" % "ffmpeg" % (ffmpegVersion + "-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "ffmpeg" % (ffmpegVersion + "-" + javacppVersion) classifier platform,
  "org.scalafx" %% "scalafx" % "8.0.60-R9"
)

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.ivy2/local/org.goseumdochi")))
