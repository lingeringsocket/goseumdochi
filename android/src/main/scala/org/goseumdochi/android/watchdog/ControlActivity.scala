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
import org.goseumdochi.android.R

import android._
import android.content._
import android.graphics._
import android.hardware._
import android.os._
import android.preference._
import android.view._

import java.io._
import java.util._

import android.hardware.Camera

import org.goseumdochi.vision._
import org.goseumdochi.control._

import akka.actor._

import com.typesafe.config._

import com.orbotix._
import com.orbotix.common._
import com.orbotix.common.RobotChangedStateListener._

import collection._

object ControlActivity
{
  private final val INITIAL_STATUS = "CONNECTED"

  private final val SPEECH_RESOURCE_PREFIX = "speech_"

  private var systemId = 0

  private def nextId() =
  {
    systemId += 1
    systemId
  }
}

import ControlActivity._

class ControlActivity extends ActivityBaseNoCompat
    with RobotChangedStateListener with SensorEventListener
    with ConvenienceRobotProvider
{
  private var robot : Option[ConvenienceRobot] = None

  private val outputQueue =
    new java.util.concurrent.ArrayBlockingQueue[Bitmap](1)

  private val retinalInput = new AndroidRetinalInput

  private lazy val controlView =
    new ControlView(this, retinalInput, outputQueue)

  private lazy val preview = new CameraPreview(this, controlView)

  private lazy val theater = createTheater

  private val actuator = new AndroidSpheroActuator(this)

  private lazy val actorSystem = ActorSystem(
    "AndroidActors" + ControlActivity.nextId,
    ConfigFactory.load("android.conf"))

  private var controlActorOpt : Option[ActorRef] = None

  private var controlStatus = INITIAL_STATUS

  private var lastVoiceMessage = ""

  private var discoveryStarted = false

  private var connectionStatus = "WAITING FOR CONNECTION"

  private var sensorMgr : Option[SensorManager] = None

  private var gyroscope : Option[Sensor] = None

  private var gyroscopeBaseline = new mutable.ArrayBuffer[Float]

  private var detectBumps = false

  private var expectDisconnect = false

  private var found = false

  private var videoFileTheater : Option[VideoFileTheater] = None

  private val connectionTimer = new Timer("Bluetooth Connection Timeout", true)

  private lazy val videoMode = readVideoMode

  private lazy val videoModeNone = getString(
      R.string.pref_val_video_trigger_none)
  private lazy val videoModeFull = getString(
      R.string.pref_val_video_trigger_before_initialization_start)
  private lazy val videoModeAfterInitialization = getString(
      R.string.pref_val_video_trigger_after_initialization_complete)
  private lazy val videoModeFirstIntruder = getString(
      R.string.pref_val_video_trigger_first_intruder)

  class ControlListener extends Actor
  {
    def receive =
    {
      case msg : ControlActor.StatusUpdateMsg => {
        if (msg.status == ControlActor.ControlStatus.ACTIVE) {
          if (videoMode == videoModeAfterInitialization) {
            videoFileTheater.foreach(_.enable())
          }
        }
        controlStatus = msg.status.toString
        var actualMessage = msg.messageKey
        if (msg.messageKey == "INTRUDER") {
          if (videoMode == videoModeFirstIntruder) {
            videoFileTheater.foreach(_.enable())
          }
          val prefs = PreferenceManager.getDefaultSharedPreferences(
            ControlActivity.this)
          val defaultValue = getString(R.string.pref_default_intruder_alert)
          actualMessage = prefs.getString(
            SettingsActivity.PREF_INTRUDER_ALERT, defaultValue)
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
            SettingsActivity.applyFormat(
              ControlActivity.this, actualMessage,
              msg.messageParams)
        }
        speak(actualMessage)
        if (msg.status == ControlActor.ControlStatus.ORIENTING) {
          found = true
        }
        if (msg.status == ControlActor.ControlStatus.LOST) {
          if (found) {
            finishWithError(classOf[LostActivity])
          } else {
            finishWithError(classOf[UnfoundActivity])
          }
        }
      }
    }
  }

  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)

    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    detectBumps = prefs.getBoolean(
      SettingsActivity.PREF_DETECT_BUMPS, true)

    if (detectBumps) {
      val sysSensorMgr =
        getSystemService(Context.SENSOR_SERVICE).asInstanceOf[SensorManager]
      sensorMgr = Some(sysSensorMgr)
      gyroscope =
        Option(sysSensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE))
    }

    speak(R.string.speech_bluetooth_connection)

    getWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    getWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    startCamera
  }

  private def startCamera()
  {
    setContentView(R.layout.control)
    val layout = findView(TR.control_preview)
    // this will cause instantiation of (lazy) preview and controlView
    layout.addView(preview)
    layout.addView(controlView)
    controlView.setOnTouchListener(controlView)
    findView(TR.control_linear_layout).bringToFront
  }

  private def startDiscovery()
  {
    expectDisconnect = false
    if (!discoveryStarted) {
      DualStackDiscoveryAgent.getInstance.startDiscovery(
        getApplicationContext)
      connectionTimer.schedule(new TimerTask {
        override def run()
        {
          handleRobotChangedState(
            null,
            RobotChangedStateNotificationType.FailedConnect)
        }
      }, 20000)
      discoveryStarted = true
    }
  }

  override def handleRobotChangedState(
    r : Robot,
    notification : RobotChangedStateNotificationType)
  {
    if (expectDisconnect) {
      return
    }
    notification match {
      case RobotChangedStateNotificationType.Online => {
        connectionTimer.cancel
        robot = Some(new ConvenienceRobot(r))
        val file = new File(
          getApplicationContext.getFilesDir, "orientation.ser")
        System.setProperty(
          "GOSEUMDOCHI_ORIENTATION_FILE",
          file.getAbsolutePath)
        val props = Props(
          classOf[ControlActor],
          actuator,
          Props(classOf[VisionActor], retinalInput, theater))
        val controlActor = actorSystem.actorOf(
          props, ControlActor.CONTROL_ACTOR_NAME)
        controlActorOpt = Some(controlActor)
        ControlActor.addListener(
          controlActor,
          actorSystem.actorOf(
            Props(classOf[ControlListener], this), "statusActor"))
      }
      case RobotChangedStateNotificationType.Disconnected => {
        if (!robot.isEmpty) {
          connectionStatus = "CONNECTION LOST"
          speak(R.string.speech_bluetooth_lost)
          robot = None
          finishWithError(classOf[BluetoothErrorActivity])
        }
      }
      case RobotChangedStateNotificationType.FailedConnect => {
        connectionStatus = "FAILED"
        speak(R.string.speech_bluetooth_failed)
        finishWithError(classOf[BluetoothErrorActivity])
      }
      case _ =>
    }
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
    DualStackDiscoveryAgent.getInstance.addRobotStateListener(this)
    startDiscovery
    sensorMgr.foreach(sm => {
      gyroscope.foreach(sensor => {
        sm.registerListener(
          this, sensor, SensorManager.SENSOR_DELAY_UI)
      })
    })
  }

  override protected def onPause()
  {
    super.onPause
    pencilsDown
    if (!isFinishing) {
      finish
    }
  }

  private def pencilsDown()
  {
    connectionStatus = "DISCONNECTED"
    expectDisconnect = true
    connectionTimer.cancel
    sensorMgr.foreach(_.unregisterListener(this))
    gyroscope = None
    gyroscopeBaseline.clear
    videoFileTheater.foreach(GlobalVideo.closeTheater(_))
    videoFileTheater = None
    controlActorOpt.foreach(controlActor => {
      actorSystem.stop(controlActor)
      actorSystem.shutdown
    })
    controlActorOpt = None
    if (DualStackDiscoveryAgent.getInstance.isDiscovering) {
      DualStackDiscoveryAgent.getInstance.stopDiscovery
    }
    robot.foreach(_.disconnect)
    robot = None
    preview.closeCamera
  }

  def isRobotConnected = !robot.isEmpty

  override def getRobot = robot

  def getRobotState = {
    if (isRobotConnected) {
      controlStatus
    } else {
      connectionStatus
    }
  }

  def getVoiceMessage = lastVoiceMessage

  def getTheaterListener = theater.getListener

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
      finishWithError(classOf[BumpActivity])
    }
  }

  private def finishWithError(errorClass : Class[_])
  {
    val intent = new Intent(this, errorClass)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    pencilsDown
    finish
    startActivity(intent)
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

  override def onAccuracyChanged(sensor : Sensor, accuracy : Int)
  {
  }

  private def readVideoMode() =
  {
    val prefs = PreferenceManager.getDefaultSharedPreferences(
      ControlActivity.this)
    prefs.getString(
      SettingsActivity.PREF_VIDEO_TRIGGER, videoModeNone)
  }

  private def createTheater() : RetinalTheater =
  {
    val androidTheater = new AndroidTheater(controlView, outputQueue)
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
