name := "goseumdochi-base"

organization := "org.goseumdochi"

version := "0.1"

isSnapshot := true

exportJars := true

scalaVersion := "2.11.7"

val javacppVersion = "1.2"

val opencvVersion = "3.1.0"

val ffmpegVersion = "3.0.2"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint",
  "-Xfatal-warnings", "-Ywarn-unused-import")

scalacOptions in Test ++= Seq("-Yrangepos")

lazy val platform = org.bytedeco.javacpp.Loader.getPlatform

classpathTypes += "maven-plugin"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

resolvers += "bintray/meetup" at "http://dl.bintray.com/meetup/maven"

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
  "com.meetup" %% "archery" % "0.4.0",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "org.bytedeco"                 % "javacpp"         % "1.2.1" % "test",
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) % "test" classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) % "test" classifier platform,
  "org.bytedeco.javacpp-presets" % "ffmpeg" % (ffmpegVersion + "-" + javacppVersion) % "test" classifier "",
  "org.bytedeco.javacpp-presets" % "ffmpeg" % (ffmpegVersion + "-" + javacppVersion) % "test" classifier platform,
  "org.bytedeco"                 % "javacv"          % javacppVersion,
  "com.owlike" %% "genson-scala" % "1.4",
  "com.typesafe.akka"      %% "akka-actor"     % "2.3.15",
  "com.typesafe.akka"      %% "akka-testkit"     % "2.3.15" % "test",
  "com.jsuereth" %% "scala-arm" % "1.4",
  "org.specs2"        %% "specs2-core"             % "3.7.2"           % "test"
)

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
