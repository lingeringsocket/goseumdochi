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

import scala.concurrent.duration._

object BirdsEyeOrientationFsm
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
  // * ControlActor.AnxietyMsg

  // states
  case object Blind extends State
  case object WaitingForStart extends State
  case object WaitingForEnd extends State
  case object Done extends State

  // data
  case object Empty extends Data
  final case class StartPoint(
    pos : PlanarPos, startTime : TimePoint) extends Data
}
import BirdsEyeOrientationFsm._

class BirdsEyeOrientationFsm()
    extends BehaviorFsm[State, Data]
{
  private val panicDelay = settings.Control.panicDelay

  private val motionThreshold = settings.Orientation.motionThreshold

  private val forwardImpulse =
    PolarImpulse(settings.Motor.defaultSpeed, 500.milliseconds, 0)

  startWith(Blind, Empty)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg(_, eventTime), _) => {
      sender ! ControlActor.UseVisionAnalyzersMsg(Seq(
        settings.BodyRecognition.className),
        eventTime)
      sender ! ControlActor.UseVisionAugmentersMsg(Seq(
        classOf[CrosshairsGuideline].getName),
        eventTime)
      goto(WaitingForStart)
    }
  }

  when(WaitingForStart) {
    case Event(msg : MotionDetector.MotionDetectedMsg, _) => {
      stay
    }
    case Event(ControlActor.BodyMovedMsg(pos, eventTime), _) => {
      sender ! ControlActor.ActuateImpulseMsg(forwardImpulse, eventTime)
      goto(WaitingForEnd) using StartPoint(pos, eventTime)
    }
  }

  when(WaitingForEnd) {
    case Event(msg : MotionDetector.MotionDetectedMsg, _) => {
      stay
    }
    case Event(
      ControlActor.BodyMovedMsg(endPos, eventTime),
      StartPoint(startPos, startTime)) =>
    {
      val predictedMotion = predictMotion(forwardImpulse)
      val actualMotion = polarMotion(startPos, endPos)
      if (actualMotion.distance < motionThreshold) {
        if (eventTime > (startTime + panicDelay)) {
          sender ! ControlActor.AnxietyMsg(eventTime)
          goto(Done)
        } else {
          stay
        }
      } else {
        val bodyMapping = BodyMapping(
          actualMotion.distance / predictedMotion.distance,
          normalizeRadians(actualMotion.theta - predictedMotion.theta))
        sender ! ControlActor.CalibratedMsg(
          bodyMapping, FlipRetinalTransform, eventTime)
        goto(Done)
      }
    }
  }

  when(Done) {
    case _ => {
      stay
    }
  }

  initialize()
}
