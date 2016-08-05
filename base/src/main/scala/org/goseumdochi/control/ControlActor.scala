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
import org.goseumdochi.perception._

import akka.actor._
import akka.event._
import akka.routing._

import scala.concurrent.duration._

object ControlActor
{
  object ControlStatus extends Enumeration {
    type ControlStatus = Value
    val LOCALIZING, ORIENTING, ACTIVE, PANIC, RECOVERED, LOST = Value
  }
  import ControlStatus._

  // sent messages (to VisionActor)
  // VisionActor.ActivateAnalyzersMsg
  // VisionActor.ActivateAugmentersMsg

  // sent messages (to behavior)
  final case class CameraAcquiredMsg(
    bottomRight : RetinalPos, eventTime : TimePoint)
      extends EventMsg
  final case class BodyMovedMsg(
    pos : PlanarPos, eventTime : TimePoint)
      extends EventMsg
  final case class PanicAttackMsg(
    lastImpulse : Option[PolarImpulse],
    eventTime : TimePoint)
      extends EventMsg

  // sent messages (to listeners)
  final case class StatusUpdateMsg(
    status : ControlStatus,
    messageKey : String,
    eventTime : TimePoint,
    messageParams : Seq[Any] = Seq.empty)
      extends EventMsg

  // internal messages
  final case class CheckVisibilityMsg(
    eventTime : TimePoint)
      extends EventMsg

  // received messages
  // VisionActor.DimensionsKnownMsg
  // VisionActor.RequireLightMsg
  final case class CalibratedMsg(
    bodyMapping : BodyMapping,
    xform : RetinalTransform,
    eventTime : TimePoint)
      extends EventMsg
  final case class ActuateImpulseMsg(
    impulse : PolarImpulse,
    eventTime : TimePoint)
      extends EventMsg
  final case class ActuateMoveMsg(
    from : PlanarPos,
    to : PlanarPos,
    speed : Double,
    extraTime : TimeSpan,
    eventTime : TimePoint)
      extends EventMsg
  final case class ActuateTwirlMsg(
    theta : Double,
    duration : TimeSpan,
    eventTime : TimePoint)
      extends EventMsg
  final case class ActuateLightMsg(
    color : LightColor,
    eventTime : TimePoint)
      extends EventMsg
  final case class UseVisionAnalyzersMsg(
    analyzerClassNames : Seq[String],
    eventTime : TimePoint)
      extends EventMsg
  final case class UseVisionAugmentersMsg(
    augmenterClassNames : Seq[String],
    eventTime : TimePoint)
      extends EventMsg
  final case class ObservationMsg(
    messageKey : String,
    eventTime : TimePoint,
    messageParams : Seq[Any] = Seq.empty)
      extends EventMsg
  final case class PanicEndMsg(
    eventTime : TimePoint)
      extends EventMsg

  // pass-through messages (from vision to behavior)
  // any kind of VisionActor.ObjDetectedMsg

  final val CONTROL_ACTOR_NAME = "controlActor"

  final val VISION_ACTOR_NAME = "visionActor"

  final val LOCALIZATION_ACTOR_NAME = "localizationActor"

  final val ORIENTATION_ACTOR_NAME = "orientationActor"

  final val BEHAVIOR_ACTOR_NAME = "behaviorActor"

  final val PANIC_ACTOR_NAME = "panicActor"

  final val LOST_ACTOR_NAME = "lostActor"

  def readOrientation(settings : Settings) : (RetinalTransform, BodyMapping) =
  {
    val seq = PerceptualLog.deserialize(settings.Orientation.persistenceFile)
    seq.head.msg match {
      case msg : CalibratedMsg => {
        (msg.xform, BodyMapping(msg.bodyMapping.scale, 0.0))
      }
    }
  }

  def writeOrientation(settings : Settings, msg : CalibratedMsg)
  {
    val file = settings.Orientation.persistenceFile
    if (!file.isEmpty) {
      PerceptualLog.serialize(
        file,
        Seq(PerceptualEvent("orientation", "controlActor", "futureSelf", msg)))
    }
  }

  def addListener(controlActor : ActorRef, listener : ActorRef)
  {
    controlActor ! Listen(listener)
  }

  def messageKeyFor(status : ControlStatus) = status.toString
}

class ControlActor(
  actuator : Actuator,
  visionProps : Props)
    extends Actor with Listeners
{
  import ControlActor._
  import ControlStatus._
  import context.dispatcher

  private val log = Logging(context.system, this)

  private val settings = ActorSettings(context)

  private val monitorVisibility = settings.Control.monitorVisibility

  private val testsActive = settings.Test.active

  private val visionActor = context.actorOf(
    visionProps,
    VISION_ACTOR_NAME)
  private val localizationActor = context.actorOf(
    Props(Class.forName(settings.Orientation.localizationClassName)),
    LOCALIZATION_ACTOR_NAME)
  private val orientationActor = context.actorOf(
    Props(Class.forName(settings.Orientation.className)),
    ORIENTATION_ACTOR_NAME)
  private val behaviorActor = context.actorOf(
    Props(Class.forName(settings.Behavior.className)),
    BEHAVIOR_ACTOR_NAME)
  private val panicActor = context.actorOf(
    Props(Class.forName(settings.Control.panicClassName)),
    PANIC_ACTOR_NAME)
  private val lostActor = context.actorOf(
    Props(classOf[NullActor]),
    LOST_ACTOR_NAME)

  private var modeActor = localizationActor

  private val doOrientation = settings.Control.orient

  private var movingUntil = TimePoint.ZERO

  private var bodyMappingOpt : Option[BodyMapping] = None

  private var lastImpulse : Option[PolarImpulse] = None

  private var retinalTransform : RetinalTransform = {
    if (doOrientation) {
      FlipRetinalTransform
    } else {
      val (xform, bodyMapping) = ControlActor.readOrientation(settings)
      bodyMappingOpt = Some(bodyMapping)
      xform
    }
  }

  private var lastSeenTime = TimePoint.ZERO

  private var lastSeenPos = PlanarPos(0, 0)

  private var bottomRight = RetinalPos(100, 100)

  private val panicDelay = settings.Control.panicDelay

  private val visibilityCheckFreq = settings.Control.visibilityCheckFreq

  private val sensorDelay = settings.Vision.sensorDelay

  private val maxMoveDuration = settings.Control.maxMoveDuration

  private val perception = new PlanarPerception(settings)

  private var status = LOCALIZING

  private def getActorString(ref : ActorRef) = ref.path.name

  override def receive = LoggingReceive({
    case eventMsg : EventMsg => {
      if (sender != self) {
        perception.processEvent(
          PerceptualEvent(
            "", getActorString(sender), getActorString(self), eventMsg))
      }
      receiveInput(eventMsg)
    }
    case m : Any => {
      listenerManagement(m)
    }
  })

  private def sendOutput(receiver : ActorRef, msg : EventMsg) =
  {
    perception.processEvent(
      PerceptualEvent(
        "", getActorString(self), getActorString(receiver), msg))
    receiver ! msg
  }

  private def receiveInput(eventMsg : EventMsg) = eventMsg match
  {
    case calibratedMsg : CalibratedMsg => {
      val bodyMapping = calibratedMsg.bodyMapping
      retinalTransform = calibratedMsg.xform
      bodyMappingOpt = Some(BodyMapping(bodyMapping.scale, 0.0))
      val spinDuration = 500.milliseconds
      actuator.actuateTwirl(bodyMapping.thetaOffset, spinDuration, true)
      orientationActor ! PoisonPill.getInstance
      writeOrientation(settings, calibratedMsg)
      startActiveBehavior(calibratedMsg.eventTime)
    }
    case ActuateLightMsg(color : LightColor, eventTime) => {
      actuator.actuateLight(color)
    }
    case ActuateImpulseMsg(impulse, eventTime) => {
      actuateImpulse(impulse, eventTime)
    }
    case ActuateMoveMsg(from, to, speed, extraTime, eventTime) => {
      val impulse = bodyMapping.computeImpulse(from, to, speed, extraTime)
      val cappedImpulse = {
        if (impulse.duration > maxMoveDuration) {
          PolarImpulse(impulse.speed, maxMoveDuration, impulse.theta)
        } else {
          impulse
        }
      }
      // maybe we should interpolate HintBodyLocationMsgs along
      // the way as well?
      actuateImpulse(cappedImpulse, eventTime)
      sendOutput(
        visionActor, VisionActor.GoalLocationMsg(Some(to), eventTime))
    }
    case ActuateTwirlMsg(theta, duration, eventTime) => {
      actuator.actuateTwirl(theta, duration, false)
    }
    case VisionActor.DimensionsKnownMsg(pos, eventTime) => {
      bottomRight = pos
      lastSeenTime = eventTime
      enterMode(LOCALIZING, eventTime)
      sendOutput(localizationActor, CameraAcquiredMsg(bottomRight, eventTime))
    }
    case VisionActor.RequireLightMsg(color, eventTime) => {
      actuator.actuateLight(color)
    }
    // note that this pattern needs to be matched BEFORE the
    // generic ObjDetectedMsg case
    case BodyDetector.BodyDetectedMsg(pos, eventTime) => {
      if (eventTime > movingUntil) {
        val moveMsg = BodyMovedMsg(pos, eventTime)
        sendOutput(modeActor, moveMsg)
      }
      lastSeenPos = pos
      lastSeenTime = eventTime
    }
    case objectDetected : VisionActor.ObjDetectedMsg => {
      if ((objectDetected.eventTime > movingUntil) && (status != PANIC)) {
        sendOutput(modeActor, objectDetected)
      }
    }
    case UseVisionAnalyzersMsg(analyzers, eventTime) => {
      lastSeenTime = eventTime
      sendOutput(
        visionActor,
        VisionActor.ActivateAnalyzersMsg(
          analyzers, retinalTransform, eventTime))
    }
    case UseVisionAugmentersMsg(augmenters, eventTime) => {
      sendOutput(
        visionActor,
        VisionActor.ActivateAugmentersMsg(augmenters, eventTime))
    }
    case observation : ObservationMsg => {
      gossip(StatusUpdateMsg(
        status, observation.messageKey, observation.eventTime,
        observation.messageParams))
    }
    case VisionActor.HintBodyLocationMsg(pos, eventTime) => {
      lastSeenTime = eventTime
      lastSeenPos = pos
      if (status == LOCALIZING) {
        if (doOrientation) {
          enterMode(ORIENTING, eventTime)
          sendOutput(
            orientationActor, CameraAcquiredMsg(bottomRight, eventTime))
        } else {
          startActiveBehavior(eventTime)
        }
        localizationActor ! PoisonPill.getInstance
      }
      sendOutput(visionActor, VisionActor.HintBodyLocationMsg(pos, eventTime))
    }
    case PanicEndMsg(eventTime) => {
      enterMode(ACTIVE, eventTime)
    }
    case CheckVisibilityMsg(checkTime) => {
      if (checkTime < movingUntil) {
        // still moving
      } else {
        if (lastSeenTime == TimePoint.ZERO) {
          // never seen
        } else {
          if ((checkTime - lastSeenTime) > panicDelay) {
            if (status == ACTIVE) {
              enterMode(PANIC, checkTime)
              sendOutput(behaviorActor, PanicAttackMsg(lastImpulse, checkTime))
              sendOutput(panicActor, PanicAttackMsg(lastImpulse, checkTime))
            } else if ((status == LOCALIZING) || (status == ORIENTING)) {
              modeActor ! PoisonPill.getInstance
              enterMode(LOST, checkTime)
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
    case _ =>
  }

  private def startActiveBehavior(eventTime : TimePoint)
  {
    enterMode(ACTIVE, eventTime)
    sendOutput(
      visionActor,
      VisionActor.ActivateAugmentersMsg(
        Seq(classOf[RetinalTransformGuideline].getName), eventTime))
    sendOutput(
      behaviorActor, CameraAcquiredMsg(bottomRight, eventTime))
  }

  private def enterMode(newStatus : ControlStatus, eventTime : TimePoint)
  {
    modeActor = newStatus match {
      case ORIENTING => orientationActor
      case ACTIVE => behaviorActor
      case PANIC => panicActor
      case LOST => lostActor
      case _ => localizationActor
    }
    log.info("NEW STATUS:  " + newStatus)
    val messageKey = {
      if ((status == PANIC) && (newStatus == ACTIVE)) {
        messageKeyFor(RECOVERED)
      } else {
        messageKeyFor(newStatus)
      }
    }
    status = newStatus
    gossip(StatusUpdateMsg(status, messageKey, eventTime))
  }

  private def bodyMapping = bodyMappingOpt.get

  private def actuateImpulse(impulse : PolarImpulse, eventTime : TimePoint)
  {
    lastImpulse = Some(impulse)
    // FIXME:  get rid of the testsActive clause after regenerating
    // test scenarios
    if (testsActive || (status != LOCALIZING)) {
      movingUntil = eventTime + impulse.duration + sensorDelay
    }
    if (!testsActive && retinalTransform.isMirrorWorld) {
      val flipped = PolarImpulse(impulse.speed, impulse.duration, -impulse.theta)
      actuator.actuateMotion(flipped)
    } else {
      actuator.actuateMotion(impulse)
    }
  }

  override def preStart()
  {
    VisionActor.startFrameGrabber(visionActor, self)
    if (monitorVisibility) {
      context.system.scheduler.scheduleOnce(visibilityCheckFreq) {
        self ! CheckVisibilityMsg(TimePoint.now)
      }
    }
  }

  override def postRestart(reason: Throwable)
  {
  }

  override def postStop()
  {
    perception.end()
  }
}
