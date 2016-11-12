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
import org.goseumdochi.vision._

import akka.actor._

import android.graphics.drawable._
import android.hardware._
import android.os._
import android.view._

import scala.concurrent.duration._

object LeashControlActivity
{
  object LeashState extends Enumeration {
    type LeashState = Value
    val CONNECTING, ORIENTING, SITTING, TWIRLING, WALKING, RUNNING,
      STOPPING = Value
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

  private var walkingSpeed = 0.0

  private var runningSpeed = 0.0

  private var lastTime = 0L

  private var state = CONNECTING

  private var color = NamedColor.BLACK

  // 10 ms
  private val SENSOR_INTERVAL = 10000

  private var localizing = true

  private var active = false

  private lazy val connectImg = findView(TR.connect_animation_image)

  private lazy val connectAnimation =
    connectImg.getBackground.asInstanceOf[AnimationDrawable]

  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    initSensorMgr()
    sensorMgr.foreach(mgr => {
      rotationVector =
        Option(mgr.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR))
      if (rotationVector.isEmpty) {
        rotationVector =
          Option(mgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR))
      }
      accelerometer =
        Option(mgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION))
    })
    walkingSpeed = LeashSettingsActivity.getWalkingSpeed(this)
    runningSpeed = LeashSettingsActivity.getRunningSpeed(this)
    if (rotationVector.isEmpty || accelerometer.isEmpty) {
      finishWithError(classOf[LeashNoSensorActivity])
    }
    connectImg.setBackgroundResource(R.drawable.connect_animation)
    connectAnimation.start
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

  override protected def onResume()
  {
    super.onResume
    LeashAnalytics.trackScreen("Control")
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
      case Sensor.TYPE_ROTATION_VECTOR | Sensor.TYPE_GAME_ROTATION_VECTOR => {
        level = ((Math.abs(event.values(0)) < 0.1) &&
          (Math.abs(event.values(1)) < 0.1))
        rotationLatest = 2.0*Math.asin(event.values(2))
        if (rotationBaseline == Double.MaxValue) {
          rotationBaseline = rotationLatest
          rotationLast = rotationLatest
        }
      }
      case Sensor.TYPE_LINEAR_ACCELERATION => {
        if (active) {
          if (iStart == 0) {
            iStart = event.timestamp
            lastTime = iStart
          } else {
            if (event.timestamp > lastTime) {
              accelerationEvent(event)
            }
          }
        }
      }
      case _ =>
    }
  }

  def ripen()
  {
    LeashSettingsActivity.setFeedbackRipeness(
      this, LeashSettingsActivity.FEEDBACK_RIPE)
  }

  def isUnripe() : Boolean =
  {
    val ripeness = LeashSettingsActivity.getFeedbackRipeness(this)
    return ripeness == LeashSettingsActivity.FEEDBACK_UNRIPE
  }

  private def accelerationEvent(event : SensorEvent)
  {
    val (jerkNow, acceleration) = leash.processEvent(event)

    if (!leash.isResting) {
      leash.updateVelocity(lastTime, acceleration)
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
      val newState = leash.calculateState(turnAngle)
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
          speed = runningSpeed
          changeColor(NamedColor.CYAN)
        }
        case WALKING => {
          speed = walkingSpeed
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
      if (leash.stopRequested(jerkNow, turnAngle)) {
        impulse = PolarImpulse(0, 0.seconds, leash.getLastImpulse.theta)
        yank = true
        state = STOPPING
      }
    }
    if (yank) {
      leash.rememberYank(impulse, (state == RUNNING))
      controlActorOpt.foreach(controlActor => {
        controlActor ! ControlActor.ActuateImpulseMsg(
          impulse,
          TimePoint.now)
      })
    }
    lastTime = event.timestamp
  }

  def getState = state

  def getStateText =
  {
    if (isAttaching) {
      if (isOrienting) {
        if (localizing) {
          if (level) {
            "NOW CENTER CAMERA DIRECTLY ABOVE SPHERO AND " +
              "THEN TOUCH SCREEN IN CENTER OF CROSSHAIRS"
          } else {
            "PLEASE HOLD CAMERA PARALLEL TO THE FLOOR, " +
              "ABOVE SPHERO AT WAIST HEIGHT"
          }
        } else {
          "PLEASE CONTINUE TO HOLD CAMERA MOTIONLESS..."
        }
      } else {
        getRobotState
      }
    } else {
      state match {
        case SITTING => "READY TO FOLLOW YOU"
        case WALKING => "WALKING (HOLD PHONE MOTIONLESS TO STOP)"
        case RUNNING => "RUNNING (HOLD PHONE MOTIONLESS TO STOP)"
        case _ => "HOLD PHONE MOTIONLESS"
      }
    }
  }

  override def getRotationCompensation =
  {
    normalizeRadians(rotationLatest - rotationBaseline)
  }

  override protected def handleConnectionEstablished()
  {
    super.handleConnectionEstablished
    connectAnimation.stop
    connectImg.setVisibility(View.GONE)
    state = ORIENTING
    actuator.setMotionTimeout(10.seconds)
    LeashAnalytics.trackEvent("status", "CONNECTED")
  }

  def getActuator = actuator

  override protected def handleConnectionFailed()
  {
    super.handleConnectionFailed
    finishWithError(classOf[LeashBluetoothErrorActivity])
  }

  override protected def handleConnectionLost()
  {
    super.handleConnectionLost
    state = CONNECTING
    finishWithError(classOf[LeashBluetoothErrorActivity])
  }

  override protected def handleStatusUpdate(msg : ControlActor.StatusUpdateMsg)
  {
    super.handleStatusUpdate(msg)
    if (msg.status == ControlActor.ControlStatus.ACTIVE) {
      controlActorOpt.foreach(controlActor => {
        controlActor ! VisionActor.CloseEyesMsg(
          TimePoint.now)
      })
      changeColor(NamedColor.MAGENTA)
      state = SITTING
      active = true
      val restThreshold = {
        if (walkingSpeed < 0.25) {
          (VirtualLeash.SEVEN_TENTHS_SEC / (4*walkingSpeed)).toLong
        } else {
          VirtualLeash.SEVEN_TENTHS_SEC
        }
      }
      leash = new VirtualLeash(restThreshold)
    }
    if (msg.status == ControlActor.ControlStatus.LOST) {
      finishWithError(classOf[LeashUnfoundActivity])
    }
    if (msg.status == ControlActor.ControlStatus.ORIENTING) {
      localizing = false
    }
    LeashAnalytics.trackEvent("status", msg.status.toString)
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

  def isOrienting = (state == ORIENTING)

  def isConnecting = (state == CONNECTING)

  def isAttaching = (isConnecting || isOrienting)

  def getLeash = leash
}
