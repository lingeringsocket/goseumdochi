name := "goseumdochi-root"

organization := Common.organization

version := Common.version

isSnapshot := true

scalaVersion := Common.scalaVersion

scalacOptions := Common.scalacOptions

classpathTypes += "maven-plugin"

autoCompilerPlugins := true

parallelExecution in Test := false

fork := true

javaOptions += Common.javaOptions

lazy val base = project

lazy val `desktop-sphero` = project.
  dependsOn(base % "compile->compile;test->test")

lazy val root = (project in file(".")).
  aggregate(base, `desktop-sphero`).
  dependsOn(`desktop-sphero`)

lazy val android = project.dependsOn(base)

lazy val watchdog = project.enablePlugins(AndroidApp).dependsOn(android)

lazy val leash = project.enablePlugins(AndroidApp).dependsOn(android)

mainClass in Compile := Some("org.goseumdochi.ConsoleMain")

resolvers ++= Common.resolvers

libraryDependencies ++= Seq(
  "org.scalafx" %% "scalafx" % "8.0.60-R9"
)

libraryDependencies ++= Common.javacvDeps

libraryDependencies ++= Common.javacvPlatformDeps("runtime")

libraryDependencies ++= Common.ffmpegPlatformDeps("runtime")

maxErrors := Common.maxErrors

traceLevel := Common.traceLevel

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.ivy2/local/org.goseumdochi")))
