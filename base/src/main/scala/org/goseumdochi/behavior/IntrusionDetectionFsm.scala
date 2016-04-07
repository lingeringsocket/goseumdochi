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

import scala.concurrent.duration._

object IntrusionDetectionFsm
{
  sealed trait State
  sealed trait Data

  // received messages
  // * ControlActor.CameraAcquiredMsg
  // * ControlActor.BodyMovedMsg
  // * MotionDetector.MotionDetectedMsg

  // sent messages
  // * ControlActor.UseVisionAnalyzersMsg
  // * ControlActor.ActuateMoveMsg

  // states
  case object Blind extends State
  case object WaitingForIntruder extends State
  case object ManeuveringToIntruder extends State

  // data
  case object Empty extends Data
  final case class IntruderAt(
    pos : PlanarPos
  ) extends Data
}
import IntrusionDetectionFsm._
import MotionDetector._

class IntrusionDetectionFsm()
    extends BehaviorFsm[State, Data]
{
  private val settings = ActorSettings(context)

  startWith(Blind, Empty)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg(_, eventTime), _) => {
      sender ! ControlActor.UseVisionAnalyzersMsg(Seq(
        settings.BodyRecognition.className,
        settings.Behavior.intrusionDetectorClassName),
        eventTime)
      goto(WaitingForIntruder)
    }
  }

  when(WaitingForIntruder) {
    case Event(MotionDetectedMsg(pos, _), _) => {
      goto(ManeuveringToIntruder) using IntruderAt(pos)
    }
    case Event(msg : ControlActor.BodyMovedMsg, _) => {
      stay
    }
  }

  when(ManeuveringToIntruder) {
    case Event(
      ControlActor.BodyMovedMsg(pos, eventTime),
      IntruderAt(intruderPos)) =>
    {
      sender ! ControlActor.ActuateMoveMsg(
        pos, intruderPos, settings.Motor.defaultSpeed,
        0.seconds, eventTime)
      goto(WaitingForIntruder) using Empty
    }
    case Event(MotionDetectedMsg(pos, _), _) => {
      stay
    }
  }

  whenUnhandled {
    case Event(msg : ControlActor.PanicAttackMsg, _) => {
      goto(WaitingForIntruder) using Empty
    }
    case event => handleUnknown(event)
  }

  initialize()
}
