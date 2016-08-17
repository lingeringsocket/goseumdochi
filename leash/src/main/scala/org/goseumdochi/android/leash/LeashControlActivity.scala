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

package org.goseumdochi.android.leash

import org.goseumdochi.android.lib._
import org.goseumdochi.behavior._
import org.goseumdochi.common._
import org.goseumdochi.common.MoreMath._
import org.goseumdochi.control._

import akka.actor._

import android.hardware._
import android.os._

import scala.concurrent.duration._

object LeashControlActivity
{
  object LeashState extends Enumeration {
    type LeashState = Value
    val ATTACHING, SITTING, TWIRLING, WALKING, RUNNING, STOPPING = Value
  }
}

class LeashControlActivity extends ControlActivityBase
    with TypedFindView
{
  import LeashControlActivity._
  import LeashState._

  private var accelerometer : Option[Sensor] = None

  private var rotationVector : Option[Sensor] = None

  private var leash = new VirtualLeash(VirtualLeash.THREE_SEC)

  private var iStart = 0L

  private var rotationLast : Double = Double.MaxValue

  private var rotationLatest : Double = Double.MaxValue

  private var rotationBaseline : Double = Double.MaxValue

  private var level = false

  private var waitingForLevel = false

  private var urgeSpeed = 0.0

  private var lastTime = 0L

  private var state = ATTACHING

  private var color = NamedColor.BLACK

  // 10 ms
  private val SENSOR_INTERVAL = 10000

  private var orienting = false

  private var active = false

  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    initSensorMgr()
    sensorMgr.foreach(mgr => {
      rotationVector =
        Option(mgr.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR))
      accelerometer =
        Option(mgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION))
    })
  }

  override protected def onStart()
  {
    super.onStart
    sensorMgr.foreach(mgr => {
      Seq(rotationVector, accelerometer).flatten.foreach(sensor => {
        mgr.registerListener(
          this, sensor, SENSOR_INTERVAL)
      })
    })
  }

  override protected def createControlView() =
  {
    new LeashControlView(this, retinalInput, outputQueue)
  }

  override protected def startCamera()
  {
    setContentView(R.layout.control)
    val layout = findView(TR.control_preview)
    // this will cause instantiation of (lazy) preview and controlView
    layout.addView(preview)
    layout.addView(controlView)
    controlView.setOnTouchListener(controlView)
    findView(TR.control_relative_layout).bringToFront
  }

  override protected def pencilsDown()
  {
    super.pencilsDown
    rotationVector = None
    rotationBaseline = Double.MaxValue
    accelerometer = None
  }

  override def onSensorChanged(event : SensorEvent)
  {
    event.sensor.getType match {
      case Sensor.TYPE_GAME_ROTATION_VECTOR => {
        level = ((Math.abs(event.values(0)) < 0.1) &&
          (Math.abs(event.values(1)) < 0.1))
        rotationLatest = 2.0*Math.asin(event.values(2))
        if (rotationBaseline == Double.MaxValue) {
          rotationBaseline = rotationLatest
          rotationLast = rotationLatest
        }
      }
      case Sensor.TYPE_LINEAR_ACCELERATION => {
        if (iStart == 0) {
          iStart = event.timestamp
          lastTime = iStart
        } else {
          if (event.timestamp > lastTime) {
            accelerationEvent(event)
          }
        }
      }
      case _ =>
    }
  }

  private def accelerationEvent(event : SensorEvent)
  {
    val (iSample, jerkNow, acceleration) = leash.processEvent(event)

    if (!active) {
      if (leash.isResting(iSample)) {
        if (level && waitingForLevel) {
          waitingForLevel = false
          controlActorOpt.foreach(controlActor => {
            controlActor !
              CenterLocalizationFsm.BodyCenteredMsg(TimePoint.now)
          })
        }
      }
      return
    }

    if (!leash.isResting(iSample)) {
      leash.updateVelocity(iSample, lastTime, acceleration)
    } else {
      leash.clear
      rotationLast = rotationLatest
      if (leash.isRobotStopped) {
        changeColor(NamedColor.MAGENTA)
        state = SITTING
      }
    }

    var impulse = PolarImpulse(0, 0.seconds, leash.getLastImpulse.theta)
    var yank = false
    val turnAngle = normalizeRadians(rotationLast - rotationLatest)

    if (state == SITTING) {
      val newState = leash.calculateState(iSample, turnAngle)
      if (state != newState) {
        rotationLast = rotationLatest
      }
      state = newState
      var speed = 0.0
      state match {
        case TWIRLING => {
          changeColor(NamedColor.BLUE)
        }
        case RUNNING => {
          speed = 2*urgeSpeed
          changeColor(NamedColor.CYAN)
        }
        case WALKING => {
          speed = urgeSpeed
          changeColor(NamedColor.GREEN)
        }
        case _ =>
      }
      if (speed > 0) {
        yank = true
        impulse = PolarImpulse(
          speed,
          TimeSpans.INDEFINITE,
          normalizeRadians(leash.getPeakMotion.theta))
      }
    }
    if (state != STOPPING) {
      if (leash.stopRequested(iSample, jerkNow, turnAngle)) {
        impulse = PolarImpulse(0, 0.seconds, leash.getLastImpulse.theta)
        yank = true
        state = STOPPING
      }
    }
    if (yank) {
      leash.rememberYank(iSample, impulse)
      controlActorOpt.foreach(controlActor => {
        controlActor ! ControlActor.ActuateImpulseMsg(
          impulse,
          TimePoint.now)
      })
    }
    lastTime = iSample
  }

  def getForce = leash.getForce

  def getState = {
    if (state == ATTACHING) {
      if (orienting) {
        if (waitingForLevel) {
          "PLEASE HOLD CAMERA DIRECTLY ABOVE SPHERO"
        } else {
          "PLEASE WAIT..."
        }
      } else {
        getRobotState
      }
    } else {
      state.toString
    }
  }

  override def getRotationCompensation =
  {
    normalizeRadians(rotationLatest - rotationBaseline)
  }

  override protected def handleConnectionEstablished()
  {
    super.handleConnectionEstablished
    orienting = true
    waitingForLevel = true
    urgeSpeed = settings.Motor.defaultSpeed
    actuator.setMotionTimeout(10.seconds)
  }

  override protected def handleConnectionFailed()
  {
    super.handleConnectionFailed
    finishWithError(classOf[LeashBluetoothErrorActivity])
  }

  override protected def handleConnectionLost()
  {
    super.handleConnectionLost
    state = ATTACHING
    finishWithError(classOf[LeashBluetoothErrorActivity])
  }

  override protected def handleStatusUpdate(msg : ControlActor.StatusUpdateMsg)
  {
    super.handleStatusUpdate(msg)
    if (msg.status == ControlActor.ControlStatus.ACTIVE) {
      orienting = false
      changeColor(NamedColor.MAGENTA)
      state = SITTING
      active = true
      leash = new VirtualLeash(VirtualLeash.SEVEN_TENTHS_SEC)
    }
  }

  private def changeColor(newColor : LightColor)
  {
    if (color != newColor) {
      color = newColor
      controlActorOpt.foreach(controlActor => {
        controlActor ! ControlActor.ActuateLightMsg(
          color,
          TimePoint.now)
      })
    }
  }

  def isOrienting = orienting

}
