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

package org.goseumdochi.android

import android.app._
import android.content._
import android.graphics._
import android.hardware._
import android.os._
import android.preference._
import android.view._

import java.io._

import android.hardware.Camera

import org.goseumdochi.vision._
import org.goseumdochi.control._

import akka.actor._

import com.typesafe.config._

import com.orbotix._
import com.orbotix.common._

import collection._

object ControlActivity
{
  private final val INITIAL_STATUS = "CONNECTED"

  private final val SPEECH_RESOURCE_PREFIX = "speech_"
}

import ControlActivity._

class ControlActivity extends Activity
    with RobotChangedStateListener with SensorEventListener with TypedFindView
{
  private var robot : Option[ConvenienceRobot] = None

  private val outputQueue =
    new java.util.concurrent.ArrayBlockingQueue[Bitmap](1)

  private val retinalInput = new AndroidRetinalInput

  private lazy val controlView =
    new ControlView(this, retinalInput, outputQueue)

  private lazy val preview = new CameraPreview(this, controlView)

  private lazy val theater = new AndroidTheater(controlView, outputQueue)

  private val actuator = new AndroidSpheroActuator(this)

  private var actorSystem : Option[ActorSystem] = None

  private var controlStatus = INITIAL_STATUS

  private var lastVoiceMessage = ""

  private var discoveryStarted = false

  private var connectionStatus = "WAITING FOR CONNECTION"

  private var sensorMgr : Option[SensorManager] = None

  private var gyroscope : Option[Sensor] = None

  private var gyroscopeBaseline = new mutable.ArrayBuffer[Float]

  private var detectBumps = false

  private var expectDisconnect = false

  class ControlListener extends Actor
  {
    def receive =
    {
      case msg : ControlActor.StatusUpdateMsg => {
        controlStatus = msg.status.toString
        var actualMessage = msg.messageKey
        if (msg.messageKey == "INTRUDER") {
          val prefs = PreferenceManager.getDefaultSharedPreferences(
            ControlActivity.this)
          val defaultValue = getString(R.string.pref_default_intruder_alert)
          actualMessage = prefs.getString(
            SettingsActivity.KEY_PREF_INTRUDER_ALERT, defaultValue)
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
      }
    }
  }

  override protected def onCreate(savedInstanceState : Bundle)
  {
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    super.onCreate(savedInstanceState)

    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    detectBumps = prefs.getBoolean(
      SettingsActivity.KEY_PREF_DETECT_BUMPS, true)

    if (detectBumps) {
      val sysSensorMgr =
        getSystemService(Context.SENSOR_SERVICE).asInstanceOf[SensorManager]
      sensorMgr = Some(sysSensorMgr)
      gyroscope =
        Option(sysSensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE))
    }

    speak(R.string.speech_bluetooth_connection)

    DualStackDiscoveryAgent.getInstance.addRobotStateListener(this)
    getWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    getWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    startCamera
  }

  private def startCamera()
  {
    setContentView(R.layout.control)
    val layout = findView(TR.control_preview)
    layout.addView(preview)
    layout.addView(controlView)
    controlView.setOnTouchListener(controlView)
  }

  override protected def onStart()
  {
    super.onStart
    expectDisconnect = false
    startDiscovery
  }

  private def startDiscovery()
  {
    if (!discoveryStarted) {
      DualStackDiscoveryAgent.getInstance.startDiscovery(
        getApplicationContext)
      discoveryStarted = true
    }
  }

  override protected def onStop()
  {
    expectDisconnect = true

    if (DualStackDiscoveryAgent.getInstance.isDiscovering) {
      DualStackDiscoveryAgent.getInstance.stopDiscovery
    }

    robot.foreach(_.disconnect)
    robot = None

    super.onStop
  }

  override protected def onDestroy()
  {
    super.onDestroy
    DualStackDiscoveryAgent.getInstance.addRobotStateListener(null)
  }

  override def handleRobotChangedState(
    r : Robot,
    notification : RobotChangedStateListener.RobotChangedStateNotificationType)
  {
    if (notification ==
      RobotChangedStateListener.RobotChangedStateNotificationType.Online)
    {
      robot = Some(new ConvenienceRobot(r))
      if (actorSystem.isEmpty) {
        val file = new File(
          getApplicationContext.getFilesDir, "orientation.ser")
        System.setProperty(
          "GOSEUMDOCHI_ORIENTATION_FILE",
          file.getAbsolutePath)
        val system = ActorSystem(
          "AndroidActors",
          ConfigFactory.load("android.conf"))
        actorSystem = Some(system)
        val props = Props(
          classOf[ControlActor],
          actuator,
          Props(classOf[VisionActor], retinalInput, theater))
        val controlActor = system.actorOf(
          props, ControlActor.CONTROL_ACTOR_NAME)
        ControlActor.addListener(
          controlActor,
          system.actorOf(Props(classOf[ControlListener], this), "statusActor"))
      }
    } else {
      if (!robot.isEmpty) {
        connectionStatus = "DISCONNECTED"
        if (!expectDisconnect) {
          speak(R.string.speech_bluetooth_lost)
        }
      }
      robot = None
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

  override protected def onResume()
  {
    super.onResume
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
    disableSensors
    preview.closeCamera
  }

  private def disableSensors()
  {
    sensorMgr.foreach(_.unregisterListener(this))
    gyroscope = None
    gyroscopeBaseline.clear
  }

  def isRobotConnected = !robot.isEmpty

  def getRobot = robot

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
      expectDisconnect = true
      disableSensors
      speak(R.string.speech_bump_detected)
      val intent = new Intent(this, classOf[BumpActivity])
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      finish
      startActivity(intent)
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

  override def onAccuracyChanged(sensor : Sensor, accuracy : Int)
  {
  }
}
