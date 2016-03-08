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

package org.goseumdochi.control

import org.goseumdochi.common._
import org.goseumdochi.vision._

import akka.actor._
import akka.routing._
import akka.event._

import scala.concurrent.duration._

object ControlActor
{
  // sent messages
  // VisionActor.ActivateAnalyzersMsg
  final case class CameraAcquiredMsg(eventTime : TimePoint)
      extends EventMsg
  final case class BodyMovedMsg(pos : PlanarPos, eventTime : TimePoint)
      extends EventMsg
  final case class PanicAttackMsg(eventTime : TimePoint)
      extends EventMsg

  // internal messages
  final case class CheckVisibilityMsg(eventTime : TimePoint)
      extends EventMsg

  // received messages
  // VisionActor.DimensionsKnownMsg
  final case class CalibratedMsg(
    bodyMapping : BodyMapping,
    xform : RetinalTransform,
    eventTime : TimePoint)
      extends EventMsg
  final case class ActuateImpulseMsg(
    impulse : PolarImpulse, eventTime : TimePoint)
      extends EventMsg
  final case class ActuateMoveMsg(
    from : PlanarPos, to : PlanarPos,
    speed : Double, extraTime : TimeSpan, eventTime : TimePoint)
      extends EventMsg
  final case class ActuateTwirlMsg(
    degrees : Int, duration : TimeSpan, eventTime : TimePoint)
      extends EventMsg
  final case class ActuateLightMsg(
    color : java.awt.Color)
  final case class UseVisionAnalyzersMsg(analyzerClassNames : Seq[String])

  // pass-through messages
  // any kind of VisionActor.ObjDetectedMsg
}

class ControlActor(
  actuator : Actuator,
  visionProps : Props,
  monitorVisibility : Boolean)
    extends Actor
{
  import ControlActor._
  import context.dispatcher

  private val log = Logging(context.system, this)

  private val settings = Settings(context)

  private val visionActor = context.actorOf(
    visionProps, "visionActor")
  private val localizationActor = context.actorOf(
    Props(Class.forName(settings.Orientation.localizationClassName)),
    "localizationActor")
  private val orientationActor = context.actorOf(
    Props(Class.forName(settings.Orientation.className)),
    "orientationActor")
  private val behaviorActor = context.actorOf(
    Props(Class.forName(settings.Behavior.className)),
    "behaviorActor")

  private var localizing = true

  private var orienting = true

  private var movingUntil = TimePoint.ZERO

  private var retinalTransform : RetinalTransform =
    IdentityRetinalTransform

  private var bodyMappingOpt : Option[BodyMapping] = None

  private var lastSeenTime = TimePoint.ZERO

  private var lastSeenPos : Option[PlanarPos] = None

  private var cornerOpt : Option[RetinalPos] = None

  private val panicDelay = settings.Control.panicDelay

  private val visibilityCheckFreq =
    settings.Control.visibilityCheckFreq

  private val random = scala.util.Random

  def receive = LoggingReceive(
  {
    case CalibratedMsg(bodyMapping, xform, eventTime) => {
      retinalTransform = xform
      bodyMappingOpt = Some(bodyMapping)
      orienting = false
      behaviorActor ! CameraAcquiredMsg(eventTime)
      orientationActor ! PoisonPill.getInstance
      log.info("ORIENTATION COMPLETE")
    }
    case ActuateLightMsg(color : java.awt.Color) => {
      actuator.actuateLight(color)
    }
    case ActuateImpulseMsg(impulse, eventTime) => {
      actuateImpulse(impulse, eventTime)
    }
    case ActuateMoveMsg(from, to, speed, extraTime, eventTime) => {
      val impulse = bodyMapping.computeImpulse(from, to, speed, extraTime)
      // maybe we should interpolate HintBodyLocationMsgs along
      // the way as well?
      actuateImpulse(impulse, eventTime)
    }
    case ActuateTwirlMsg(degrees, duration, eventTime) => {
      actuator.actuateTwirl(degrees, duration)
    }
    case VisionActor.DimensionsKnownMsg(pos, eventTime) => {
      cornerOpt = Some(pos)
      localizationActor ! CameraAcquiredMsg(eventTime)
    }
    // note that this pattern needs to be matched BEFORE the
    // generic ObjDetectedMsg case
    case BodyDetector.BodyDetectedMsg(pos, eventTime) => {
      if (eventTime > movingUntil) {
        if (localizing) {
          localizationActor ! BodyMovedMsg(pos, eventTime)
        } else if (orienting) {
          orientationActor ! BodyMovedMsg(pos, eventTime)
        } else {
          behaviorActor ! BodyMovedMsg(pos, eventTime)
        }
      }
      lastSeenPos = Some(pos)
      lastSeenTime = eventTime
    }
    case objectDetected : VisionActor.ObjDetectedMsg => {
      if (localizing) {
        localizationActor ! objectDetected
      } else if (orienting) {
        orientationActor ! objectDetected
      } else if (objectDetected.eventTime > movingUntil) {
        behaviorActor ! objectDetected
      }
    }
    case UseVisionAnalyzersMsg(analyzers) => {
      visionActor ! VisionActor.ActivateAnalyzersMsg(
        analyzers, retinalTransform)
    }
    case VisionActor.HintBodyLocationMsg(pos, eventTime) => {
      if (localizing) {
        localizing = false
        orientationActor ! CameraAcquiredMsg(eventTime)
        localizationActor ! PoisonPill.getInstance
        log.info("BODY LOCATED")
      }
      visionActor ! VisionActor.HintBodyLocationMsg(pos, eventTime)
    }
    case CheckVisibilityMsg(checkTime) => {
      val randomColor = {
        if (random.nextBoolean) {
          new java.awt.Color(
            random.nextInt(256), random.nextInt(256), random.nextInt(256))
        } else {
          java.awt.Color.BLACK
        }
      }
      actuator.actuateLight(randomColor)
      if (checkTime < movingUntil) {
        // still moving
      } else {
        if (lastSeenTime == TimePoint.ZERO) {
          // never seen
        } else {
          if ((checkTime - lastSeenTime) > panicDelay) {
            if (orienting) {
              // not much we can do yet
            } else {
              log.info("PANIC!")
              behaviorActor ! PanicAttackMsg(checkTime)
              val from = lastSeenPos.get
              val to = retinalTransform.retinaToWorld(
                RetinalPos(corner.x / 2.0, corner.y / 2.0))
              val impulse = bodyMapping.computeImpulse(
                from, to, settings.Motor.defaultSpeed, 0.milliseconds)
              actuateImpulse(impulse, checkTime)
            }
          } else {
            // all is well
          }
        }
      }
      if (monitorVisibility) {
        context.system.scheduler.scheduleOnce(visibilityCheckFreq) {
          self ! CheckVisibilityMsg(TimePoint.now)
        }
      }
    }
  })

  private def bodyMapping = bodyMappingOpt.get

  private def corner = cornerOpt.get

  private def actuateImpulse(impulse : PolarImpulse, eventTime : TimePoint)
  {
    val sensorDelay = Settings(context).Vision.sensorDelay
    movingUntil = eventTime + impulse.duration + sensorDelay
    actuator.actuateMotion(impulse)
  }

  override def preStart()
  {
    actuator.actuateLight(java.awt.Color.CYAN)
    visionActor ! Listen(self)
    if (monitorVisibility) {
      context.system.scheduler.scheduleOnce(visibilityCheckFreq) {
        self ! CheckVisibilityMsg(TimePoint.now)
      }
    }
  }

  override def postRestart(reason: Throwable)
  {
  }
}
