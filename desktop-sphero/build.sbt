name := "goseumdochi-desktop-sphero"

organization := "org.goseumdochi"

version := "0.1"

isSnapshot := true

scalaVersion := "2.11.7"

val javacppVersion = "1.2"

val opencvVersion = "3.1.0"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint",
  "-Xfatal-warnings", "-Ywarn-unused-import")

scalacOptions in Test ++= Seq("-Yrangepos")

lazy val platform = org.bytedeco.javacpp.Loader.getPlatform

classpathTypes += "maven-plugin"

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
  "com.typesafe.akka"      %% "akka-actor"     % "2.3.15",
  "com.typesafe.akka"      %% "akka-testkit"     % "2.3.15" % "test",
  "org.bytedeco"                 % "javacpp"         % "1.2.1",
  "org.bytedeco"                 % "javacv"          % javacppVersion,
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) classifier platform,
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
