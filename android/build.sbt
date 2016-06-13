import android.Keys._

android.Plugin.androidBuild

platformTarget in Android := "android-23"

name := """goseumdochi-android"""

organization := Common.organization

version := Common.version

scalaVersion := Common.scalaVersion

val javacppVersion = Common.javacppVersion

val javacppPointVersion = Common.javacppPointVersion

val opencvVersion = Common.opencvVersion

val ffmpegVersion = Common.ffmpegVersion

// FIXME:  add back in "-deprecation", "-Xfatal-warnings"
scalacOptions ++= Common.scalacOptionsAllowWarnings

autoCompilerPlugins := true

classpathTypes += "maven-plugin"

resolvers ++= Common.resolvers

libraryDependencies ++= Seq(
  aar("com.google.android" % "multidex" % "0.1"),
  aar("com.google.android.gms" % "play-services" % "4.0.30"),
  aar("com.android.support" % "support-v4" % "20.0.0"),
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) classifier "android-arm",
  "org.bytedeco.javacpp-presets" % "ffmpeg" % (ffmpegVersion + "-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "ffmpeg" % (ffmpegVersion + "-" + javacppVersion) classifier "android-arm",
  "org.slf4j" % "slf4j-android" % "1.7.21"
)

libraryDependencies ++= Common.javacvDeps

// Override the run task with the android:run
run <<= run in Android

// Activate proguard for Scala
proguardScala in Android := true

// Activate proguard for Android
useProguard in Android := true

useProguardInDebug in Android := true

dexMulti in Android := true

dexMinimizeMain in Android := true

dexMainClasses in Android := Seq(
  "org/goseumdochi/android/MultidexApplication.class",
  "java/rmi/Remote.class",
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

proguardOptions in Android ++= Seq(
  "-ignorewarnings",
  "-dontobfuscate",
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

packagingOptions in Android := PackagingOptions(merges=Seq("reference.conf"), excludes=Seq("META-INF/maven/org.bytedeco.javacpp-presets/opencv/pom.properties","META-INF/maven/org.bytedeco.javacpp-presets/opencv/pom.xml","META-INF/maven/org.bytedeco.javacpp-presets/ffmpeg/pom.properties","META-INF/maven/org.bytedeco.javacpp-presets/ffmpeg/pom.xml", "META-INF/MANIFEST.MF", "META-INF/LICENSE.txt", "META-INF/LICENSE", "META-INF/NOTICE.txt", "META-INF/NOTICE"))

javacOptions ++= Seq("-target", "1.7", "-source", "1.7")
