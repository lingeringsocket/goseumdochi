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

package org.goseumdochi.common

import akka.actor._
import com.typesafe.config._
import java.util.concurrent._
import scala.concurrent.duration._

class Settings(rootConf : Config)
{
  private val conf = rootConf.getConfig("goseumdochi")

  private def getMillis(subConf: Config, path : String) =
    TimeSpan(subConf.getDuration(path, TimeUnit.MILLISECONDS), MILLISECONDS)

  object Bluetooth
  {
    val subConf = conf.getConfig("bluetooth")
    val debug = subConf.getBoolean("debug")
  }

  object Sphero
  {
    val subConf = conf.getConfig("sphero")
    val bluetoothId = subConf.getString("bluetooth-id")
  }

  object Vision
  {
    val subConf = conf.getConfig("vision")
    val inputClass = subConf.getString("input-class-name")
    val remoteInputUrl = subConf.getString("remote-input-url")
    val throttlePeriod = getMillis(subConf, "throttle-period")
    val sensorDelay = getMillis(subConf, "sensor-delay")
    val debugDir = subConf.getString("debug-dir")
    val transformGuidelineExpiration =
      getMillis(subConf, "transform-guideline-expiration")
  }

  object Control
  {
    val subConf = conf.getConfig("control")
    val orient = subConf.getBoolean("orient")
    val panicDelay = getMillis(subConf, "panic-delay")
    val monitorVisibility = subConf.getBoolean("monitor-visibility")
    val visibilityCheckFreq = getMillis(subConf, "visibility-check-freq")
    val panicClassName = subConf.getString("panic-class-name")
    val maxRollDuration = getMillis(subConf, "max-roll-duration")
  }

  object Behavior
  {
    val subConf = conf.getConfig("behavior")
    val className = subConf.getString("class-name")
    val intrusionDetectorClassName =
      subConf.getString("intrusion-detector-class-name")
  }

  object Perception
  {
    val subConf = conf.getConfig("perception")
    val logFile = subConf.getString("log-file")
  }

  object View
  {
    val subConf = conf.getConfig("view")
    val visualizeRetinal = subConf.getBoolean("visualize-retinal")
    val className = subConf.getString("class-name")
    val playbackRate = subConf.getDouble("playback-rate")
  }

  object Motor
  {
    val subConf = conf.getConfig("motor")
    val defaultSpeed = subConf.getDouble("default-speed")
    val fullSpeed = subConf.getDouble("full-speed")
  }

  object Orientation
  {
    val subConf = conf.getConfig("orientation")
    val className = subConf.getString("class-name")
    val localizationClassName = subConf.getString("localization-class-name")
    val quietPeriod = getMillis(subConf, "quiet-period")
    val persistenceFile = subConf.getString("persistence-file")
    val centeringUndershootFactor = subConf.getDouble(
      "centering-undershoot-factor")
  }

  object BodyRecognition
  {
    val subConf = conf.getConfig("body-recognition")
    val className = subConf.getString("class-name")
    val minRadius = subConf.getInt("min-radius")
    val maxRadius = subConf.getInt("max-radius")
  }

  object MotionDetection
  {
    val subConf = conf.getConfig("motion-detection")
    val bodyThreshold = subConf.getInt("body-threshold")
    val fineThreshold = subConf.getInt("fine-threshold")
    val coarseThreshold = subConf.getInt("coarse-threshold")
  }

  object Test
  {
    val subConf = conf.getConfig("test")
    val active = subConf.getBoolean("active")
    val visualize = subConf.getBoolean("visualize")
    val quiescencePeriod = getMillis(subConf, "quiescence-period")
  }

  def instantiateObject(className : String, args : AnyRef*) =
    Class.forName(className).getConstructors.head.
      newInstance((Seq(this) ++ args) : _*)
}

object Settings
{
  def apply(config : Config) = new Settings(config)

  def complainMissing(path : String)
  {
    throw new ConfigException.Missing(path)
  }
}

class ActorSettings(rootConf : Config, extendedSystem : ExtendedActorSystem)
    extends Settings(rootConf)
    with Extension
{
}

object ActorSettings extends ExtensionId[ActorSettings] with ExtensionIdProvider
{
  override def lookup = ActorSettings

  override def createExtension(system : ExtendedActorSystem) =
    new ActorSettings(system.settings.config, system)

  def apply(context : ActorContext) : ActorSettings = apply(context.system)
}
