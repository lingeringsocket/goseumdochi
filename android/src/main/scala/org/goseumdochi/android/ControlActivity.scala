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

import android._
import android.app._
import android.bluetooth._
import android.content._
import android.content.pm._
import android.graphics._
import android.os._
import android.view._
import android.widget._
import android.speech.tts._

import java.io._
import java.util._

import android.hardware.Camera

import org.goseumdochi.vision._
import org.goseumdochi.control._

import akka.actor._

import com.typesafe.config._

import com.orbotix._
import com.orbotix.common._

class ControlActivity extends Activity with RobotChangedStateListener
{
  private final val PERMISSION_REQUEST = 42

  private final val ENABLE_BT_REQUEST = 43

  private final val INITIAL_STATUS = "CONNECTED"

  private var robot : Option[ConvenienceRobot] = None

  private val outputQueue =
    new java.util.concurrent.ArrayBlockingQueue[Bitmap](1)

  private val retinalInput = new AndroidRetinalInput

  private lazy val controlView =
    new ControlView(this, retinalInput, outputQueue)

  lazy val theater = new AndroidTheater(controlView, outputQueue)

  private val actuator = new AndroidSpheroActuator(this)

  private var actorSystem : Option[ActorSystem] = None

  private var textToSpeech : Option[TextToSpeech] = None

  private var controlStatus = INITIAL_STATUS

  private var lastVoiceMessage = ""

  private var bluetoothEnabled = false

  private var discoveryStarted = false

  private var connectionStatus = "WAITING FOR CONNECTION"

  class ControlListener extends Actor
  {
    def receive =
    {
      case ControlActor.StatusUpdateMsg(status, voiceMessage, _) => {
        controlStatus = status.toString
        speak(voiceMessage)
      }
    }
  }

  override protected def onCreate(savedInstanceState : Bundle)
  {
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    super.onCreate(savedInstanceState)

    var newTextToSpeech : TextToSpeech = null
    newTextToSpeech = new TextToSpeech(
      getApplicationContext, new TextToSpeech.OnInitListener {
        override def onInit(status : Int)
        {
          if (status != TextToSpeech.ERROR) {
            newTextToSpeech.setLanguage(Locale.UK)
            textToSpeech = Some(newTextToSpeech)
            speak("Establishing Bluetooth connection.")
          }
        }
      })

    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter
    if (!bluetoothAdapter.isEnabled) {
      val intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
      startActivityForResult(intent, ENABLE_BT_REQUEST)
    } else {
      bluetoothEnabled = true
    }

    DualStackDiscoveryAgent.getInstance.addRobotStateListener(this)
    var gotCameraPermission = true
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      gotCameraPermission = hasCameraPermission
      val gotLocationPermission = hasLocationPermission
      val gotPermissions = gotCameraPermission && gotLocationPermission
      if (!gotPermissions) {
        val permissions = new ArrayList[String]
        if (!gotCameraPermission) {
          permissions.add(Manifest.permission.CAMERA)
        }
        if (!gotLocationPermission) {
          permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        requestPermissions(
          permissions.toArray(
            new Array[String](permissions.size)),
          PERMISSION_REQUEST)
      }
    }
    getWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    getWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    if (gotCameraPermission) {
      startCamera
    }
  }

  override protected def onActivityResult(
    requestCode : Int, resultCode : Int, intent : Intent)
  {
    if (requestCode == ENABLE_BT_REQUEST) {
      if (resultCode == Activity.RESULT_OK) {
        bluetoothEnabled = true
        startDiscovery
      } else {
        connectionStatus = "BLUETOOTH NOT ENABLED"
        speak("Pretty please?")
        toastLong("Watchdog cannot run without Bluetooth enabled. ")
      }
    }
  }

  private def startCamera()
  {
    val layout = new FrameLayout(this)
    val preview = new ControlPreview(this, controlView)
    layout.addView(preview)
    layout.addView(controlView)
    setContentView(layout)
    controlView.setOnTouchListener(controlView)
  }

  private def hasCameraPermission =
    hasPermission(Manifest.permission.CAMERA)

  private def hasLocationPermission =
    hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

  private def hasPermission(permission : String) =
  {
    (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) ||
      (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
  }

  private def toastLong(msg : String)
  {
    Toast.makeText(getApplicationContext, msg, Toast.LENGTH_LONG).show
  }

  override def onRequestPermissionsResult(
    requestCode : Int, permissions : Array[String], grantResults : Array[Int])
  {
    if (requestCode == PERMISSION_REQUEST) {
      for (i <- 0 until permissions.length) {
        if (grantResults(i) != PackageManager.PERMISSION_GRANTED) {
          connectionStatus = "CANNOT CONNECT WITHOUT PERMISSION"
          speak("Pretty please?")
          toastLong(
            "Watchdog cannot run until all requested permissions " +
              "have been granted.")
          return
        }
        permissions(i) match {
          case Manifest.permission.ACCESS_COARSE_LOCATION => startDiscovery
          case Manifest.permission.CAMERA => startCamera
          case _ =>
        }
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  override protected def onStart()
  {
    super.onStart
    startDiscovery
  }

  private def startDiscovery()
  {
    if (bluetoothEnabled && !discoveryStarted && hasLocationPermission) {
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
        speak("Bluetooth connection lost.")
      }
      robot = None
    }
  }

  private def speak(voiceMessage : String)
  {
    lastVoiceMessage = voiceMessage
    textToSpeech.foreach(_.speak(voiceMessage, TextToSpeech.QUEUE_ADD, null))
  }

  override protected def onResume()
  {
    super.onResume
  }

  override protected def onPause()
  {
    super.onPause
    textToSpeech.foreach(t => {
      t.stop
      t.shutdown
    })
    textToSpeech = None
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
}
