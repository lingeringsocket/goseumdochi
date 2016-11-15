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

  def javaOptions = "-Xmx1G"

  def opencvVersion = "3.1.0"

  def javacppVersion = "1.2"

  def javacppPointVersion = "1.2.1"

  def ffmpegVersion = "3.0.2"

  def androidPlatformTarget = "android-23"

  def androidMinSdkVersion = "11"

  def androidTargetSdkVersion = "23"

  lazy val defaultPlatform = org.bytedeco.javacpp.Loader.getPlatform

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

  def javacvPlatformDeps(
    scope : String = "compile", platform : String = defaultPlatform) =
    Seq(
      "org.bytedeco.javacpp-presets" % "opencv" %
        (opencvVersion + "-" + javacppVersion) % scope classifier "",
      "org.bytedeco.javacpp-presets" % "opencv" %
        (opencvVersion + "-" + javacppVersion) % scope classifier platform)

  def ffmpegPlatformDeps(
    scope : String = "compile", platform : String = defaultPlatform) =
    Seq(
      "org.bytedeco.javacpp-presets" % "ffmpeg" %
        (ffmpegVersion + "-" + javacppVersion) % scope classifier "",
      "org.bytedeco.javacpp-presets" % "ffmpeg" %
        (ffmpegVersion + "-" + javacppVersion) % scope classifier platform)

  def maxErrors = 99

  def traceLevel = 10

  def dexMainClasses = Seq(
    "org/goseumdochi/android/lib/MultidexApplication.class",
    "android/support/multidex/BuildConfig.class",
    "android/support/multidex/MultiDex$V14.class",
    "android/support/multidex/MultiDex$V19.class",
    "android/support/multidex/MultiDex$V4.class",
    "android/support/multidex/MultiDex.class",
    "android/support/multidex/MultiDexApplication.class",
    "android/support/multidex/MultiDexExtractor$1.class",
    "android/support/multidex/MultiDexExtractor.class",
    "android/support/multidex/ZipUtil$CentralDirectory.class",
    "android/support/multidex/ZipUtil.class"
  )

  def proguardOptions = Seq(
    "-ignorewarnings",
    "-dontobfuscate",
    "-keep class com.typesafe.config.** { *; }",
    "-keep class org.goseumdochi.** { *; }",
    "-keep class scala.Dynamic",
    "-keep class scala.Option",
    "-keep class scala.Tuple*",
    "-keep class scala.PartialFunction",
    "-keep class scala.Function*",
    "-keep @org.bytedeco.javacpp.annotation interface * {*;}",
    "-keep @org.bytedeco.javacpp.annotation.Platform public class *",
    "-keepclasseswithmembernames class * {@org.bytedeco.* <fields>;}",
    "-keepclasseswithmembernames class * {@org.bytedeco.* <methods>;}",
    "-keepattributes EnclosingMethod",
    "-keep @interface org.bytedeco.javacpp.annotation.*,javax.inject.*",
    "-keepattributes *Annotation*, Exceptions, Signature, Deprecated, SourceFile, SourceDir, LineNumberTable, LocalVariableTable, LocalVariableTypeTable, Synthetic, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations, AnnotationDefault, InnerClasses",
    "-keep class com.orbotix.** {*;}",
    "-keep class org.bytedeco.javacpp.* {*;}",
    "-keep class org.bytedeco.javacpp.helper.* {*;}",
    "-dontwarn java.awt.**",
    "-dontwarn org.bytedeco.javacv.**",
    "-dontwarn org.bytedeco.javacpp.**",
    "-keep class akka.actor.LightArrayRevolverScheduler { *; }",
    "-keep class akka.actor.LocalActorRefProvider { *; }",
    "-keep class akka.actor.CreatorFunctionConsumer { *; }",
    "-keep class akka.actor.TypedCreatorFunctionConsumer { *; }",
    "-keep class akka.dispatch.BoundedDequeBasedMessageQueueSemantics { *; }",
    "-keep class akka.dispatch.UnboundedMessageQueueSemantics { *; }",
    "-keep class akka.dispatch.UnboundedDequeBasedMessageQueueSemantics { *; }",
    "-keep class akka.dispatch.DequeBasedMessageQueueSemantics { *; }",
    "-keep class akka.dispatch.MultipleConsumerSemantics { *; }",
    "-keep class akka.actor.LocalActorRefProvider$Guardian { *; }",
    "-keep class akka.actor.LocalActorRefProvider$SystemGuardian { *; }",
    "-keep class akka.dispatch.UnboundedMailbox { *; }",
    "-keep class akka.actor.DefaultSupervisorStrategy { *; }",
    "-keep class macroid.akka.AkkaAndroidLogger { *; }",
    "-keep class akka.event.Logging$LogExt { *; }"
  )

  def packagingExcludes = Seq(
    "META-INF/maven/org.bytedeco.javacpp-presets/opencv/pom.properties",
    "META-INF/maven/org.bytedeco.javacpp-presets/opencv/pom.xml",
    "META-INF/maven/org.bytedeco.javacpp-presets/ffmpeg/pom.properties",
    "META-INF/maven/org.bytedeco.javacpp-presets/ffmpeg/pom.xml",
    "META-INF/MANIFEST.MF",
    "META-INF/LICENSE.txt",
    "META-INF/LICENSE",
    "META-INF/NOTICE.txt",
    "META-INF/NOTICE")
}
