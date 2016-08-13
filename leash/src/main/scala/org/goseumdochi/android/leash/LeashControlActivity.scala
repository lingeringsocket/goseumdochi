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

  private var acceleration = PlanarFreeVector(0, 0)

  private var velocity = PlanarFreeVector(0, 0)

  private var peakForce = PlanarFreeVector(0, 0)

  private var peakLock = false

  private var jerk = false

  private var jerkLast = false

  private var twirl = false

  private var iStart = 0L

  private var iLastRest = 0L

  private var iLastMotion = 0L

  private var iLastMotionStart = 0L

  private var restingMax = 0.2

  private var force = PlanarFreeVector(0, 0)

  private var lastImpulse = PolarImpulse(0, 0.seconds, 0)

  private var lastYank = 0L

  private var rotationVector : Option[Sensor] = None

  private var rotationLast : Double = Double.MaxValue

  private var rotationLatest : Double = Double.MaxValue

  private var rotationBaseline : Double = Double.MaxValue

  private var urgeSpeed = 0.0

  private var lastTime = 0L

  private var state = ATTACHING

  private var color = NamedColor.BLACK

  // 10 ms
  private val SENSOR_INTERVAL = 10000

  private var orienting = false

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
      rotationVector.foreach(sensor => {
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
    val TENTH_SEC = 10000000L
    val HALF_SEC = TENTH_SEC*5
    val ONE_SEC = HALF_SEC*2

    val iSample = event.timestamp
    acceleration = PlanarFreeVector(
      event.values(0).toDouble,
      event.values(1).toDouble)
    val polar = polarMotion(acceleration)
    val magnitude = polar.distance
    var jerkNow = false
    if (false) {
      restingMax = Math.max(restingMax, 1.2*magnitude)
      iLastRest = iSample
    } else {
      if (magnitude > restingMax) {
        iLastMotion = iSample
        if (iLastMotionStart == 0) {
          iLastMotionStart = iLastMotion
        }
        if ((magnitude > 7.0) && !peakLock) {
          jerkNow = true
        }
        if (magnitude > 10.0) {
          jerkNow = true
        }
        if (!peakLock && jerkNow) {
          jerk = true
        }
      } else {
        if ((iSample - iLastMotion) > 7*TENTH_SEC) {
          iLastRest = iSample
        }
      }
    }
    if (iSample > iLastRest) {
      val dT = (iSample - lastTime) / 1000000.0
      velocity = vectorSum(
        vectorScaled(acceleration, dT), velocity)
    } else {
      velocity = PlanarFreeVector(0, 0)
      peakForce = velocity
      iLastMotionStart = 0
      peakLock = false
      jerk = false
      jerkLast = false
      twirl = false
      rotationLast = rotationLatest
      if (lastImpulse.speed == 0) {
        state = SITTING
      }
    }

    var impulse = PolarImpulse(0, 0.seconds, lastImpulse.theta)
    var yank = false
    val turnAngle = normalizeRadians(rotationLast - rotationLatest)

    if (state == SITTING) {
      val robotForce = PlanarFreeVector(-velocity.y , velocity.x)
      val motion = polarMotion(robotForce)
      var peakMotion = polarMotion(peakForce)
      if (!peakLock && (motion.distance > peakMotion.distance)) {
        peakForce = robotForce
        peakMotion = polarMotion(peakForce)
      }
      force = PlanarFreeVector(peakForce.x , -peakForce.y)
      val dotProduct = vectorDot(robotForce, peakForce)
      val strength = peakMotion.distance
      val big = (strength > 300.0)
      if (big &&
        ((dotProduct < 0) || (motion.distance < 0.6*strength)))
      {
        peakLock = true
      }
      val sustained = (iLastMotion - iLastMotionStart) > 3*TENTH_SEC
      val sufficientPause = (iSample - lastYank) > HALF_SEC
      if (big && sustained && sufficientPause
        && (lastImpulse.speed == 0) && !twirl)
      {
        rotationLast = rotationLatest
        if (!jerk && (Math.abs(turnAngle) > 0.25*HALF_PI)) {
          twirl = true
          changeColor(NamedColor.BLUE)
          state = TWIRLING
        } else {
          yank = true
          var speed = urgeSpeed
          if (jerk) {
            speed = speed*2
            jerkLast = true
            changeColor(NamedColor.CYAN)
            state = RUNNING
          } else {
            changeColor(NamedColor.GREEN)
            state = WALKING
          }
          impulse = PolarImpulse(
            speed,
            TimeSpans.INDEFINITE,
            normalizeRadians(peakMotion.theta))
        }
      }
    }
    if (state != STOPPING) {
      if (lastImpulse.speed > 0) {
        val jerkStop = (!jerkLast && jerkNow)
        val jerkExpired = (jerkLast && ((iSample - lastYank) > (10*ONE_SEC)))
        val jerkFresh = (jerkLast && ((iSample - lastYank) < (5*ONE_SEC)))
        val restStop = ((iSample == iLastRest) && !jerkFresh)
        val turnStop = (Math.abs(turnAngle) > 0.5*HALF_PI)
        if (restStop || jerkExpired || jerkStop || turnStop) {
          impulse = PolarImpulse(0, 0.seconds, lastImpulse.theta)
          yank = true
          state = STOPPING
        }
      }
    }
    if (yank) {
      lastYank = iSample
      lastImpulse = impulse
      controlActorOpt.foreach(controlActor => {
        controlActor ! ControlActor.ActuateImpulseMsg(
          lastImpulse,
          TimePoint.now)
      })
    }
    lastTime = iSample
  }

  def getForce = force

  def getState = {
    if (state == ATTACHING) {
      if (orienting) {
        "PLEASE HOLD CAMERA DIRECTLY ABOVE SPHERO"
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
      sensorMgr.foreach(mgr => {
        accelerometer.foreach(sensor => {
          mgr.registerListener(
            this, sensor, SENSOR_INTERVAL)
        })
      })
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
