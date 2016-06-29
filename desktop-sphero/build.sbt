name := "goseumdochi-desktop-sphero"

organization := Common.organization

version := Common.version

isSnapshot := true

scalaVersion := Common.scalaVersion

scalacOptions ++= Common.scalacOptions

scalacOptions in Test ++= Seq("-Yrangepos")

classpathTypes += "maven-plugin"

resolvers ++= Common.resolvers

libraryDependencies ++= Common.javacvDeps

libraryDependencies ++= Common.javacvPlatformDeps("runtime")

libraryDependencies ++= Common.specs2Deps

libraryDependencies ++= Common.akkaDeps

autoCompilerPlugins := true

parallelExecution in Test := false

fork := true

javaOptions += Common.javaOptions

maxErrors := Common.maxErrors

traceLevel := Common.traceLevel

scalastyleFailOnError := true

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.ivy2/local/org.goseumdochi")))
