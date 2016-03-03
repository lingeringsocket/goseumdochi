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
import org.goseumdochi.control._
import org.goseumdochi.vision._

import akka.actor._

import scala.math._
import org.goseumdochi.common.MoreMath._

import scala.concurrent.duration._

// FIXME:  split off body detection phase from orientation phase,
// and get rid of "Calibration" terminology

object ProjectiveCalibrationFsm
{
  sealed trait State
  sealed trait Data

  // received messages
  // * ControlActor.CameraAcquiredMsg
  // * ControlActor.BodyMovedMsg
  // * MotionDetector.MotionDetectedMsg

  // sent messages
  // * VisionActor.ActivateAnalyzersMsg
  // * VisionActor.HintBodyLocationMsg
  // * ControlActor.CalibratedMsg
  // * ControlActor.ActuateImpulseMsg

  // states
  case object Blind extends State
  case object WaitingForQuiet extends State
  case object FindingBody extends State
  case object Aligning extends State
  case object Measuring extends State
  case object Done extends State

  // data
  case object Empty extends Data
  final case class Alignment(
    lastTheta : Double,
    lastPos : PlanarPos,
    scale : Double = 0.0) extends Data
  final case class WithControl(
    controlActor : ActorRef,
    eventTime : TimePoint) extends Data
}
import ProjectiveCalibrationFsm._

class ProjectiveCalibrationFsm()
    extends BehaviorFsm[State, Data]
{
  private val settings = Settings(context)

  private val quietPeriod = settings.Calibration.quietPeriod

  private val forwardImpulse =
    PolarImpulse(settings.Motor.defaultSpeed, 1500.milliseconds, 0)

  private val backwardImpulse =
    PolarImpulse(settings.Motor.defaultSpeed, 800.milliseconds, Pi)

  startWith(Blind, Empty)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg(eventTime), _) => {
      sender ! VisionActor.ActivateAnalyzersMsg(Seq(
        settings.BodyRecognition.className,
        classOf[FineMotionDetector].getName))
      goto(WaitingForQuiet) using WithControl(sender, eventTime + quietPeriod)
    }
  }

  when(WaitingForQuiet, stateTimeout = quietPeriod) {
    case Event(StateTimeout, WithControl(controlActor, eventTime)) => {
      controlActor ! ControlActor.ActuateImpulseMsg(
        backwardImpulse, eventTime)
      goto(FindingBody)
    }
    case _ => {
      stay
    }
  }

  // FIXME:  add a StateTimeout for moving around more aggressively
  when(FindingBody) {
    case Event(MotionDetector.MotionDetectedMsg(pos, eventTime), _) => {
      sender ! VisionActor.HintBodyLocationMsg(pos, eventTime)
      startAlignment(pos, eventTime)
    }
    case Event(ControlActor.BodyMovedMsg(pos, eventTime), _) => {
      startAlignment(pos, eventTime)
    }
  }

  when(Aligning) {
    case Event(msg : MotionDetector.MotionDetectedMsg, _) => {
      stay
    }
    case Event(ControlActor.BodyMovedMsg(pos, eventTime), a : Alignment) => {
      val motion = polarMotion(a.lastPos, pos)
      val SMALL_ANGLE = 0.1
      if (motion.distance < 0.1) {
        stay
      } else if (abs(motion.theta) < SMALL_ANGLE) {
        val newTheta = a.lastTheta + HALF_PI
        val newImpulse = forwardImpulse.copy(theta = newTheta)
        val predictedMotion = predictMotion(forwardImpulse)
        val scale = motion.distance / predictedMotion.distance
        sender ! ControlActor.ActuateImpulseMsg(
          newImpulse, eventTime)
        goto(Measuring) using Alignment(a.lastTheta, pos, scale)
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
        val newImpulse = forwardImpulse.copy(theta = newTheta)
        sender ! ControlActor.ActuateImpulseMsg(
          newImpulse, eventTime)
        stay using Alignment(normalizeRadians(newTheta), pos)
      }
    }
  }

  when(Measuring) {
    case Event(msg : MotionDetector.MotionDetectedMsg, _) => {
      stay
    }
    case Event(ControlActor.BodyMovedMsg(pos, eventTime), a : Alignment) => {
      val motion = polarMotion(a.lastPos, pos)
      val SMALL_ANGLE = 0.1
      if (motion.distance < 0.1) {
        stay
      } else {
        val predictedMotion = predictMotion(forwardImpulse)
        val vscale = motion.distance / predictedMotion.distance
        println("ANGLE = " + motion.theta)
        println("HSCALE = " + a.scale)
        println("VSCALE = " + vscale)
        val bodyMapping = BodyMapping(a.scale, a.lastTheta)
        sender ! ControlActor.CalibratedMsg(bodyMapping, eventTime)
        goto(Done) using Empty
      }
    }
  }

  when(Done) {
    case _ => {
      stay
    }
  }

  initialize()

  private def startAlignment(pos : PlanarPos, eventTime : TimePoint) =
  {
    sender ! VisionActor.ActivateAnalyzersMsg(Seq(
      settings.BodyRecognition.className))
    sender ! ControlActor.ActuateImpulseMsg(
      forwardImpulse, eventTime)
    goto(Aligning) using Alignment(0.0, pos)
  }
}
