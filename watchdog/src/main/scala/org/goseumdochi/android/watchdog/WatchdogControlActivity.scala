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

package org.goseumdochi.android.watchdog

import org.goseumdochi.android._
import org.goseumdochi.android.lib._
import org.goseumdochi.android.R
import org.goseumdochi.android.TR

import android._
import android.hardware._
import android.os._
import android.preference._

import android.Manifest
import android.hardware.Camera

import org.goseumdochi.vision._
import org.goseumdochi.control._

import scala.collection._

object WatchdogControlActivity
{
  private final val SPEECH_RESOURCE_PREFIX = "speech_"
}

import WatchdogControlActivity._

class WatchdogControlActivity extends ControlActivityBase
    with TypedFindView
{
  private var lastVoiceMessage = ""

  private var gyroscope : Option[Sensor] = None

  private var gyroscopeBaseline = new mutable.ArrayBuffer[Float]

  private var detectBumps = false

  private var found = false

  private var videoFileTheater : Option[VideoFileTheater] = None

  private lazy val videoMode = readVideoMode

  private lazy val videoModeNone = getString(
      R.string.pref_val_video_trigger_none)
  private lazy val videoModeFull = getString(
      R.string.pref_val_video_trigger_before_initialization_start)
  private lazy val videoModeAfterInitialization = getString(
      R.string.pref_val_video_trigger_after_initialization_complete)
  private lazy val videoModeFirstIntruder = getString(
      R.string.pref_val_video_trigger_first_intruder)

  override protected def handleStatusUpdate(msg : ControlActor.StatusUpdateMsg)
  {
    super.handleStatusUpdate(msg)
    if (msg.status == ControlActor.ControlStatus.ACTIVE) {
      if (videoMode == videoModeAfterInitialization) {
        videoFileTheater.foreach(_.enable())
      }
    }
    var actualMessage = msg.messageKey
    if (msg.messageKey == "INTRUDER") {
      if (videoMode == videoModeFirstIntruder) {
        videoFileTheater.foreach(_.enable())
      }
      val prefs = PreferenceManager.getDefaultSharedPreferences(
        WatchdogControlActivity.this)
      val defaultValue = getString(R.string.pref_default_intruder_alert)
      actualMessage = prefs.getString(
        WatchdogSettingsActivity.PREF_INTRUDER_ALERT, defaultValue)
    } else {
      val resourceName = SPEECH_RESOURCE_PREFIX + msg.messageKey
      val resourceId = getResources.getIdentifier(
        resourceName, "string", getPackageName)
      if (resourceId != 0) {
        actualMessage = getString(resourceId)
      }
    }
    if (!msg.messageParams.isEmpty) {
      actualMessage =
        WatchdogSettingsActivity.applyFormat(
          WatchdogControlActivity.this, actualMessage,
          msg.messageParams)
    }
    speak(actualMessage)
    if (msg.status == ControlActor.ControlStatus.ORIENTING) {
      found = true
    }
    if (msg.status == ControlActor.ControlStatus.LOST) {
      if (found) {
        finishWithError(classOf[WatchdogLostActivity])
      } else {
        finishWithError(classOf[WatchdogUnfoundActivity])
      }
    }
  }

  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)

    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    detectBumps = prefs.getBoolean(
      WatchdogSettingsActivity.PREF_DETECT_BUMPS, true)

    if (detectBumps) {
      initSensorMgr()
      gyroscope =
        Option(sensorMgr.get.getDefaultSensor(Sensor.TYPE_GYROSCOPE))
    }

    speak(R.string.speech_bluetooth_connection)
  }

  override protected def createControlView() =
  {
    new WatchdogControlView(this, retinalInput, outputQueue)
  }

  override protected def startCamera()
  {
    setContentView(R.layout.control)
    val layout = findView(TR.control_preview)
    // this will cause instantiation of (lazy) preview and controlView
    layout.addView(preview)
    layout.addView(controlView)
    controlView.setOnTouchListener(controlView)
    findView(TR.control_linear_layout).bringToFront
  }

  override protected def handleConnectionLost()
  {
    super.handleConnectionLost
    speak(R.string.speech_bluetooth_lost)
    finishWithError(classOf[WatchdogBluetoothErrorActivity])
  }

  override protected def handleConnectionFailed()
  {
    super.handleConnectionFailed
    speak(R.string.speech_bluetooth_failed)
    finishWithError(classOf[WatchdogBluetoothErrorActivity])
  }

  private def speak(voiceMessage : String)
  {
    lastVoiceMessage = voiceMessage
    GlobalTts.speak(voiceMessage)
  }

  private def speak(voiceMessageId : Int)
  {
    speak(getString(voiceMessageId))
  }

  override protected def onStart()
  {
    super.onStart
    sensorMgr.foreach(sm => {
      gyroscope.foreach(sensor => {
        sm.registerListener(
          this, sensor, SensorManager.SENSOR_DELAY_UI)
      })
    })
  }

  override protected def pencilsDown()
  {
    super.pencilsDown
    gyroscope = None
    gyroscopeBaseline.clear
    videoFileTheater.foreach(GlobalVideo.closeTheater(_))
    videoFileTheater = None
  }

  def getVoiceMessage = lastVoiceMessage

  override def onSensorChanged(event : SensorEvent)
  {
    if (!isRobotConnected) {
      return
    }
    if (gyroscope.isEmpty) {
      return
    }
    var bumpDetected = false
    event.sensor.getType match {
      case Sensor.TYPE_GYROSCOPE => {
        if (checkSensor(gyroscopeBaseline, event.values, 0.07f)) {
          bumpDetected = true
        }
      }
      case _ =>
    }
    if (bumpDetected) {
      speak(R.string.speech_bump_detected)
      finishWithError(classOf[WatchdogBumpActivity])
    }
  }

  private def checkSensor(
    baseline : mutable.ArrayBuffer[Float], current : Array[Float],
    threshold : Float) : Boolean =
  {
    if (baseline.isEmpty) {
      baseline ++= current
    } else {
      for (i <- 0 until 3) {
        val diff = Math.abs(baseline(i) - current(i))
        if (diff > threshold) {
          return true
        }
      }
    }
    return false
  }

  private def readVideoMode() =
  {
    val prefs = PreferenceManager.getDefaultSharedPreferences(
      WatchdogControlActivity.this)
    prefs.getString(
      WatchdogSettingsActivity.PREF_VIDEO_TRIGGER, videoModeNone)
  }

  override protected def createTheater() : RetinalTheater =
  {
    val androidTheater = super.createTheater
    if (videoMode != videoModeNone) {
      if (hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
        val fileTheater = GlobalVideo.createVideoFileTheater
        if (videoMode == videoModeFull) {
          fileTheater.enable()
        }
        videoFileTheater = Some(fileTheater)
        return new TeeTheater(Seq(androidTheater, fileTheater))
      }
    }
    androidTheater
  }
}
