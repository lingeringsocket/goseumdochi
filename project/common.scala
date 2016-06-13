// goseumdochi:  experiments with incarnation
// Copyright 2016 John V. Sichi
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import sbt._
import Keys._

object Common {
  def organization = "org.goseumdochi"

  def version = "0.1"

  def scalaVersion = "2.11.7"

  def resolvers = Seq(
    Resolver.mavenLocal,
    DefaultMavenRepository,
    Resolver.typesafeRepo("releases"),
    Resolver.typesafeRepo("snapshots"),
    Resolver.typesafeIvyRepo("snapshots"),
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    Resolver.defaultLocal,
    bintray.Opts.resolver.jcenter)

  def opencvVersion = "3.1.0"

  def javacppVersion = "1.2"

  def javacppPointVersion = "1.2.1"

  def ffmpegVersion = "3.0.2"

  def scalacOptionsAllowWarnings = Seq(
    "-unchecked", "-feature", "-Xlint", "-Ywarn-unused-import")

  def scalacOptions = scalacOptionsAllowWarnings ++
    Seq("-deprecation", "-Xfatal-warnings")

  def specs2Deps = Seq(
    "org.specs2" %% "specs2-core" % "3.7.2" % "test")

  def akkaDeps = Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3.15",
    "com.typesafe.akka" %% "akka-testkit" % "2.3.15" % "test")

  def javacvDeps = Seq(
    "org.bytedeco" % "javacv" % javacppVersion,
    "org.bytedeco" % "javacpp" % javacppPointVersion)
}
