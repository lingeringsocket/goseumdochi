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

object MotionLocalizationFsm
{
  sealed trait State
  sealed trait Data

  // received messages
  // * ControlActor.CameraAcquiredMsg
  // * ControlActor.BodyMovedMsg
  // * MotionDetector.MotionDetectedMsg

  // sent messages
  // * ControlActor.UseVisionAnalyzersMsg
  // * VisionActor.HintBodyLocationMsg
  // * ControlActor.ActuateImpulseMsg

  // states
  case object Blind extends State
  case object WaitingForQuiet extends State
  case object FindingBody extends State
  case object Done extends State

  // data
  case object Empty extends Data
  final case class WithControl(
    controlActor : ActorRef,
    eventTime : TimePoint,
    waitExpiration : TimePoint,
    forward : Boolean)
      extends Data
}
import MotionLocalizationFsm._

class MotionLocalizationFsm()
    extends BehaviorFsm[State, Data]
{
  private val quietPeriod = settings.Orientation.quietPeriod

  private val adjustedTimeout = {
    if (settings.Test.active) {
      settings.Test.quiescencePeriod*2
    } else {
      quietPeriod
    }
  }

  private val forwardImpulse =
    PolarImpulse(settings.Motor.defaultSpeed, 500.milliseconds, 0)

  private val backwardImpulse =
    PolarImpulse(settings.Motor.defaultSpeed, 500.milliseconds, PI)

  startWith(Blind, Empty)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg(_, eventTime), _) => {
      sender ! ControlActor.UseVisionAnalyzersMsg(Seq(
        classOf[FineSizeMotionDetector].getName),
        eventTime)
      val waitExpiration = eventTime + quietPeriod
      goto(WaitingForQuiet) using WithControl(
        sender, waitExpiration, waitExpiration, false)
    }
  }

  when(WaitingForQuiet, stateTimeout = adjustedTimeout) {
    case Event(
      StateTimeout, WithControl(controlActor, eventTime, waitExpiration, _)) =>
    {
      controlActor ! ControlActor.ActuateImpulseMsg(
        backwardImpulse, eventTime)
      goto(FindingBody) using WithControl(
        controlActor, eventTime + quietPeriod, waitExpiration, true)
    }
    case _ => {
      stay
    }
  }

  when(FindingBody, stateTimeout = adjustedTimeout) {
    case Event(
      msg : MotionDetector.MotionDetectedMsg,
      WithControl(_, _, waitExpiration, _)) =>
    {
      if (msg.eventTime >= waitExpiration) {
        sender ! VisionActor.HintBodyLocationMsg(msg.pos, msg.eventTime)
        goto(Done)
      } else {
        stay
      }
    }
    case Event(
      StateTimeout,
      WithControl(controlActor, eventTime, waitExpiration, forward)) =>
    {
      // we should probably try increasing the motion duration
      // in case it was initially too small to be detected
      controlActor ! ControlActor.ActuateImpulseMsg(
        if (forward) { forwardImpulse } else { backwardImpulse }, eventTime)
      stay using WithControl(
        controlActor, eventTime + quietPeriod, waitExpiration, !forward)
    }
  }

  when(Done) {
    case _ => {
      stay
    }
  }

  initialize()
}
