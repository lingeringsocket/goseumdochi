name := "goseumdochi-root"

val javacppVersion = "1.1"

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

lazy val `desktop-sphero` = project.dependsOn(base)

lazy val root = (project in file(".")).
  aggregate(base, `desktop-sphero`).
  dependsOn(`desktop-sphero`)

mainClass in Compile := Some("org.goseumdochi.ConsoleMain")

libraryDependencies ++= Seq(
  "org.bytedeco"                 % "javacpp"         % javacppVersion,
  "org.bytedeco"                 % "javacv"          % javacppVersion,
  "org.bytedeco.javacpp-presets" % "opencv" % ("3.0.0-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % ("3.0.0-" + javacppVersion) classifier platform,
  "org.scalafx" %% "scalafx" % "8.0.60-R9"
)

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.ivy2/local/org.goseumdochi")))
