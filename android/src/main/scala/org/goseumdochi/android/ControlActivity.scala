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

class ControlActivity extends Activity
    with RobotChangedStateListener with SensorEventListener with TypedFindView
{
  private final val INITIAL_STATUS = "CONNECTED"

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

  var gyroscopeMax = 0.0f

  private var detectBumps = false

  class ControlListener extends Actor
  {
    def receive =
    {
      case ControlActor.StatusUpdateMsg(status, voiceMessage, _) => {
        controlStatus = status.toString
        var actualMessage = voiceMessage
        val prefix = "INTRUDER"
        if (voiceMessage.startsWith(prefix)) {
          val prefs = PreferenceManager.getDefaultSharedPreferences(
            ControlActivity.this)
          // FIXME:  get this from XML
          val defaultValue = "Intruder detected"
          actualMessage = prefs.getString(
            SettingsActivity.KEY_PREF_INTRUDER_ALERT, defaultValue)
          if (actualMessage == defaultValue) {
            val suffix = voiceMessage.stripPrefix(prefix)
            actualMessage = actualMessage + suffix
          }
        } else {
          val resourceName = "utterance_" + voiceMessage
          val resourceId = getResources.getIdentifier(
            resourceName, "id", getPackageName)
          if (resourceId != 0) {
            actualMessage = getString(resourceId)
          }
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

    speak(R.string.utterance_bluetooth_connection)

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
        speak(R.string.utterance_bluetooth_lost)
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

  def getVisionActor = theater.getVisionActor

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
      disableSensors
      speak(R.string.utterance_bump_detected)
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
        if (diff > gyroscopeMax) {
          gyroscopeMax = diff
        }
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
