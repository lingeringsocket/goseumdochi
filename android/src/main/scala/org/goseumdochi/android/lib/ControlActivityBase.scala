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

package org.goseumdochi.android.lib

import android.graphics._
import android.os._
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

object ControlActivityBase
{
  private var systemId = 0

  private def nextId() =
  {
    systemId += 1
    systemId
  }
}

abstract class ControlActivityBase extends ActivityBaseNoCompat
    with RobotChangedStateListener with ConvenienceRobotProvider
{
  private var robot : Option[ConvenienceRobot] = None

  protected val outputQueue =
    new java.util.concurrent.ArrayBlockingQueue[Bitmap](1)

  protected val retinalInput = new AndroidRetinalInput

  protected lazy val controlView = createControlView

  private lazy val theater = createTheater

  protected lazy val preview = new CameraPreview(this, controlView)

  private val actuator = new AndroidSpheroActuator(this)

  protected lazy val actorSystem = ActorSystem(
    "AndroidActors" + ControlActivityBase.nextId,
    ConfigFactory.load("android.conf"))

  protected var controlActorOpt : Option[ActorRef] = None

  private var discoveryStarted = false

  private var expectDisconnect = false

  private val connectionTimer = new Timer("Bluetooth Connection Timeout", true)

  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    getWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    getWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    startCamera
  }

  protected def createControlView() : ControlViewBase

  protected def startCamera()

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
        handleConnectionEstablished
      }
      case RobotChangedStateNotificationType.Disconnected => {
        if (!robot.isEmpty) {
          handleConnectionLost
        }
      }
      case RobotChangedStateNotificationType.FailedConnect => {
        handleConnectionFailed
      }
      case _ =>
    }
  }

  protected def handleConnectionEstablished()
  {
  }

  protected def handleConnectionLost()
  {
    robot = None
  }

  protected def handleConnectionFailed()
  {
  }

  override protected def onStart()
  {
    super.onStart
    DualStackDiscoveryAgent.getInstance.addRobotStateListener(this)
    startDiscovery
  }

  override protected def onPause()
  {
    super.onPause
    pencilsDown
    if (!isFinishing) {
      finish
    }
  }

  protected def pencilsDown()
  {
    expectDisconnect = true
    connectionTimer.cancel
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

  def getTheaterListener = theater.getListener

  protected def createTheater() : RetinalTheater =
  {
    new AndroidTheater(controlView, outputQueue)
  }
}
