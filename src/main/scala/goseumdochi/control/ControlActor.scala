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

package goseumdochi.control

import goseumdochi.common._
import goseumdochi.vision._

import akka.actor._
import akka.pattern._
import akka.routing._
import akka.util._
import akka.event._

import scala.concurrent.duration._

object ControlActor
{
  // sent messages
  case object CameraAcquiredMsg
  final case class BodyMovedMsg(pos : PlanarPos, eventTime : Long)
  case object PanicAttack

  // internal messages
  final case class CheckVisibilityMsg(eventTime : Long)

  // received messages
  // VisionActor.DimensionsKnownMsg
  final case class CalibratedMsg(bodyMapping : BodyMapping)
  final case class ActuateImpulseMsg(impulse : PolarImpulse, eventTime : Long)
  final case class ActuateMoveMsg(
    from : PlanarPos, to : PlanarPos,
    speed : Double, extraTime : Double, eventTime : Long)
  final case class ActuateLight(
    color : java.awt.Color)

  // pass-through messages
  // VisionActor.ActivateAnalyzersMsg
  // any kind of VisionActor.ObjDetectedMsg
}

import ControlActor._

class ControlActor(
  actuator : Actuator,
  visionProps : Props,
  calibrationProps : Props,
  behaviorProps : Props,
  monitorVisibility : Boolean)
    extends Actor
{
  import ControlActor._
  import context.dispatcher

  private val visionActor = context.actorOf(
    visionProps, "visionActor")
  private val calibrationActor = context.actorOf(
    calibrationProps, "calibrationActor")
  private val behaviorActor = context.actorOf(
    behaviorProps, "behaviorActor")

  private var calibrating = true

  private var movingUntil = 0L

  private var bodyMappingOpt : Option[BodyMapping] = None

  private var lastSeenTime = 0L

  private var lastSeenPos : Option[PlanarPos] = None

  private var cornerOpt : Option[PlanarPos] = None

  private val settings = Settings(context)

  private val panicDelay = settings.Control.panicDelay

  private val visibilityCheckFreq =
    Duration(settings.Control.visibilityCheckFreq, MILLISECONDS)

  def receive = LoggingReceive(
  {
    case CalibratedMsg(bodyMapping) => {
      bodyMappingOpt = Some(bodyMapping)
      calibrating = false
      behaviorActor ! CameraAcquiredMsg
      calibrationActor ! PoisonPill.getInstance
    }
    case ActuateLight(color : java.awt.Color) => {
      actuator.actuateLight(color)
    }
    case ActuateImpulseMsg(impulse, eventTime) => {
      actuateImpulse(impulse, eventTime)
    }
    case ActuateMoveMsg(from, to, speed, extraTime, eventTime) => {
      val impulse = bodyMapping.computeImpulse(from, to, speed, extraTime)
      actuateImpulse(impulse, eventTime)
    }
    case VisionActor.DimensionsKnownMsg(pos) => {
      cornerOpt = Some(pos)
    }
    // note that this one needs to come BEFORE the
    // generic ObjDetectedMsg case
    case BodyDetector.BodyDetectedMsg(pos, eventTime) => {
      if (eventTime > movingUntil) {
        if (lastSeenTime < movingUntil) {
          actuator.actuateLight(java.awt.Color.BLUE)
        }
        if (calibrating) {
          calibrationActor ! BodyMovedMsg(pos, eventTime)
        } else {
          behaviorActor ! BodyMovedMsg(pos, eventTime)
        }
      }
      lastSeenPos = Some(pos)
      lastSeenTime = eventTime
    }
    case objectDetected : VisionActor.ObjDetectedMsg => {
      if (!calibrating && (objectDetected.eventTime > movingUntil)) {
        behaviorActor ! objectDetected
      }
    }
    case VisionActor.ActivateAnalyzersMsg(analyzers) => {
      visionActor ! VisionActor.ActivateAnalyzersMsg(analyzers)
    }
    case CheckVisibilityMsg(now) => {
      if (now < movingUntil) {
        // rolling
      } else {
        if (lastSeenTime == 0) {
          // never seen
        } else {
          if ((now - lastSeenTime) > panicDelay) {
            if (calibrating) {
              // not much we can do yet
            } else {
              behaviorActor ! PanicAttack
              val from = lastSeenPos.get
              val to = PlanarPos(corner.x / 2.0, corner.y / 2.0)
              val impulse = bodyMapping.computeImpulse(
                from, to, settings.Motor.defaultSpeed, 0.0)
              actuateImpulse(impulse, now)
            }
          } else {
            // all is well
          }
        }
      }
      if (monitorVisibility) {
        context.system.scheduler.scheduleOnce(visibilityCheckFreq) {
          self ! CheckVisibilityMsg(System.currentTimeMillis)
        }
      }
    }
  })

  private def bodyMapping = bodyMappingOpt.get

  private def corner = cornerOpt.get

  private def actuateImpulse(impulse : PolarImpulse, now : Long)
  {
    actuator.actuateLight(java.awt.Color.GREEN)
    val sensorDelay = Settings(context).Vision.sensorDelay
    movingUntil = now + (impulse.duration*1000.0).toLong + sensorDelay
    actuator.actuateMotion(impulse)
  }

  override def preStart()
  {
    actuator.actuateLight(java.awt.Color.CYAN)
    visionActor ! Listen(self)
    calibrationActor ! CameraAcquiredMsg
    if (monitorVisibility) {
      context.system.scheduler.scheduleOnce(visibilityCheckFreq) {
        self ! CheckVisibilityMsg(System.currentTimeMillis)
      }
    }
  }

  override def postRestart(reason: Throwable)
  {
  }
}
