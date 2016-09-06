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

package org.goseumdochi.behavior

import org.goseumdochi.common._
import org.goseumdochi.common.MoreMath._
import org.goseumdochi.control._
import org.goseumdochi.vision._

import akka.actor._

import scala.math._

import scala.collection.immutable._
import scala.concurrent.duration._

object PerspectiveOrientationFsm
{
  sealed trait State
  sealed trait Data

  // received messages
  // * ControlActor.CameraAcquiredMsg
  // * ControlActor.BodyMovedMsg
  // * MotionDetector.MotionDetectedMsg

  // sent messages
  // * ControlActor.UseVisionAnalyzersMsg
  // * ControlActor.UseVisionAugmentersMsg
  // * ControlActor.CalibratedMsg
  // * ControlActor.ActuateImpulseMsg

  // states
  case object Blind extends State
  case object WaitingForStart extends State
  case object Aligning extends State
  case object Centering extends State
  case object Measuring extends State
  case object Done extends State

  // data
  case object Empty extends Data
  final case class Alignment(
    lastTheta : Double,
    lastPos : PlanarPos,
    bottomRight : RetinalPos,
    scale : Double = 0.0,
    centeringUndershootFactor : Double = 1.0,
    measurements : Vector[RetinalPos] = Vector.empty) extends Data
}
import PerspectiveOrientationFsm._

class PerspectiveOrientationFsm()
    extends BehaviorFsm[State, Data]
{
  private val alignmentSmallAngle = settings.Orientation.alignmentSmallAngle

  private var retinalTransform = FlipRetinalTransform

  private val forwardImpulse =
    PolarImpulse(settings.Motor.defaultSpeed, 500.milliseconds, 0)

  private val measurementImpulses = Array(
    PolarImpulse(settings.Motor.defaultSpeed, 500.milliseconds, -HALF_PI),
    PolarImpulse(settings.Motor.defaultSpeed, 500.milliseconds, -HALF_PI),
    PolarImpulse(settings.Motor.defaultSpeed, 500.milliseconds, HALF_PI),
    PolarImpulse(settings.Motor.defaultSpeed, 500.milliseconds, 0),
    PolarImpulse(settings.Motor.defaultSpeed, 500.milliseconds, HALF_PI))

  startWith(Blind, Empty)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg(bottomRight, eventTime), _) => {
      sender ! ControlActor.UseVisionAnalyzersMsg(Seq(
        settings.BodyRecognition.className),
        eventTime)
      sender ! ControlActor.UseVisionAugmentersMsg(Seq(
        classOf[CrosshairsGuideline].getName),
        eventTime)
      goto(WaitingForStart) using Alignment(0.0, PlanarPos(0, 0), bottomRight)
    }
  }

  when(WaitingForStart) {
    case Event(ControlActor.BodyMovedMsg(pos, eventTime), a : Alignment) => {
      sender ! ControlActor.ActuateImpulseMsg(forwardImpulse, eventTime)
      goto(Aligning) using a.copy(lastPos = pos)
    }
  }

  when(Aligning) {
    case Event(ControlActor.BodyMovedMsg(pos, eventTime), a : Alignment) => {
      val motion = polarMotion(a.lastPos, pos)
      if (motion.distance < 10.0) {
        stay
      } else if (abs(motion.theta) < alignmentSmallAngle) {
        val correction = a.lastTheta - motion.theta
        val predictedMotion = predictMotion(forwardImpulse)
        val scale = motion.distance / predictedMotion.distance
        applyImpulse(forwardImpulse, correction + PI, eventTime)
        recordObservation("CENTERING", eventTime)
        goto(Centering) using Alignment(correction, pos, a.bottomRight, scale)
      } else {
        val normalizedTheta = {
          if ((motion.theta < HALF_PI) && (motion.theta > -HALF_PI)) {
            motion.theta
          } else {
            normalizeRadians(motion.theta + PI)
          }
        }
        val newTheta = normalizeRadians(
          PI + a.lastTheta - (0.8 * normalizedTheta))
        applyImpulse(forwardImpulse, newTheta, eventTime)
        stay using Alignment(normalizeRadians(newTheta), pos, a.bottomRight)
      }
    }
  }

  when(Centering) {
    case Event(ControlActor.BodyMovedMsg(pos, eventTime), a : Alignment) => {
      val motion = polarMotion(a.lastPos, pos)
      if (motion.distance < 3.0) {
        stay
      } else {
        val delta = (a.bottomRight.x / 2.0) - pos.x
        val dist = abs(delta)
        if (dist < 30.0) {
          applyImpulse(measurementImpulses.head, a.lastTheta, eventTime)
          recordObservation("MEASURING", eventTime)
          goto(Measuring) using a.copy(
            lastPos = pos,
            measurements = Vector(retinalTransform.worldToRetina(pos)))
        } else {
          val theta = {
            if (delta > 0) {
              0.0
            } else {
              PI
            }
          }
          val bodyMapping = BodyMapping(a.scale, 0.0)
          val motion = PolarVector(a.centeringUndershootFactor * dist, theta)
          val impulse = bodyMapping.transformMotion(
            motion, settings.Motor.defaultSpeed)
          applyImpulse(impulse, a.lastTheta, eventTime)
          var newUndershootFactor = a.centeringUndershootFactor * 0.8
          if (newUndershootFactor < 0.3) {
            newUndershootFactor = a.centeringUndershootFactor
          }
          stay using a.copy(
            lastPos = pos,
            centeringUndershootFactor = newUndershootFactor)
        }
      }
    }
  }

  when(Measuring) {
    case Event(ControlActor.BodyMovedMsg(pos, eventTime), a : Alignment) => {
      val motion = polarMotion(a.lastPos, pos)
      if (motion.distance < 10.0) {
        stay
      } else {
        val retinalPos = retinalTransform.worldToRetina(pos)
        if (a.measurements.size == measurementImpulses.size) {
          val nearCenter = a.measurements(0)
          val farCenter = a.measurements(1)
          val distantCenter = a.measurements(2)
          val farRight = a.measurements(4)
          val nearRight = retinalPos
          val worldDist = predictMotion(measurementImpulses.head).distance
          val perspective = RestrictedPerspectiveTransform(
            nearCenter,
            farCenter,
            distantCenter,
            nearRight,
            farRight,
            worldDist)
          // scale is already baked into perspective, so we use 1.0
          val bodyMapping = BodyMapping(1.0, -a.lastTheta)
          sender ! ControlActor.CalibratedMsg(
            bodyMapping,
            perspective,
            eventTime)
          goto(Done) using Empty
        } else {
          val impulse = measurementImpulses(a.measurements.size)
          applyImpulse(impulse, a.lastTheta, eventTime)
          stay using a.copy(
            lastPos = pos,
            measurements = a.measurements :+ retinalPos)
        }
      }
    }
  }

  when(Done) {
    case _ => {
      stay
    }
  }

  whenUnhandled {
    case Event(msg : MotionDetector.MotionDetectedMsg, _) => {
      stay
    }
    case event => handleUnknown(event)
  }

  private def applyImpulse(
    impulse : PolarImpulse,
    thetaOffset : Double,
    eventTime : TimePoint)
  {
    val newTheta = normalizeRadians(impulse.theta - thetaOffset)
    val newImpulse = impulse.copy(theta = newTheta)
    sender ! ControlActor.ActuateImpulseMsg(
      newImpulse, eventTime)
  }

  initialize()
}
