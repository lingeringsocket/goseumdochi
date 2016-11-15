import android.Keys._

android.Plugin.androidBuild

libraryProject in Android := true

platformTarget in Android := Common.androidPlatformTarget

minSdkVersion in Android := Common.androidMinSdkVersion

targetSdkVersion in Android := Common.androidTargetSdkVersion

name := """goseumdochi-android"""

organization := Common.organization

version := Common.version

scalaVersion := Common.scalaVersion

// FIXME:  add back in "-deprecation", "-Xfatal-warnings"
scalacOptions ++= Common.scalacOptionsAllowWarnings

autoCompilerPlugins := true

classpathTypes += "maven-plugin"

resolvers ++= Common.resolvers

libraryDependencies ++= Seq(
  aar("com.google.android" % "multidex" % "0.1"),
  aar("com.google.android.gms" % "play-services" % "9.2.0"),
  aar("com.android.support" % "support-v4" % "22.1.0"),
  aar("com.android.support" % "appcompat-v7" % "22.1.0"),
  "com.getkeepsafe.relinker" % "relinker" % "1.2.2",
  "org.slf4j" % "slf4j-android" % "1.7.21"
)

libraryDependencies ++= Common.javacvDeps

libraryDependencies ++= Common.javacvPlatformDeps("compile", "android-arm")

javacOptions ++= Seq("-target", "1.7", "-source", "1.7")

maxErrors := Common.maxErrors

traceLevel := Common.traceLevel
