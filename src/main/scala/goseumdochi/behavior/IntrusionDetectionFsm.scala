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

package goseumdochi.behavior

import goseumdochi.common._
import goseumdochi.control._
import goseumdochi.vision._

import akka.actor._

import goseumdochi.common.MoreMath._

object IntrusionDetectionFsm
{
  sealed trait State
  sealed trait Data

  // received messages
  // * ControlActor.CameraAcquiredMsg
  // * ControlActor.BodyMovedMsg
  // * MotionDetector.MotionDetectedMsg

  // sent messages
  // * VisionActor.ActivateAnalyzersMsg
  // * ControlActor.ActuateMoveMsg

  // states
  case object Blind extends State
  case object WaitingForIntruder extends State
  case object ManeuveringToIntruder extends State

  // data
  case object Uninitialized extends Data
  final case class IntruderAt(
    pos : PlanarPos
  ) extends Data
}
import IntrusionDetectionFsm._
import MotionDetector._

class IntrusionDetectionFsm()
    extends BehaviorFsm[State, Data]
{
  private val settings = Settings(context)

  startWith(Blind, Uninitialized)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg, _) => {
      sender ! VisionActor.ActivateAnalyzersMsg(Seq(
        settings.BodyRecognition.className,
        classOf[CoarseMotionDetector].getName))
      goto(WaitingForIntruder)
    }
  }

  when(WaitingForIntruder) {
    case Event(MotionDetectedMsg(pos, _), _) => {
      goto(ManeuveringToIntruder) using IntruderAt(pos)
    }
    case Event(ControlActor.BodyMovedMsg(_, _), _) => {
      stay
    }
  }

  when(ManeuveringToIntruder) {
    case Event(
      ControlActor.BodyMovedMsg(pos, eventTime),
      IntruderAt(intruderPos)) =>
    {
      val offset = polarMotion(pos, intruderPos)
      if (offset.distance < 30.0) {
        goto(WaitingForIntruder) using Uninitialized
      } else {
        sender ! ControlActor.ActuateMoveMsg(
          pos, intruderPos, settings.Motor.defaultSpeed, 0.2, eventTime)
        stay
      }
    }
    case Event(MotionDetectedMsg(pos, _), _) => {
      stay
    }
  }

  whenUnhandled {
    case Event(ControlActor.PanicAttack, _) => {
      goto(WaitingForIntruder) using Uninitialized
    }
    case event => handleUnknown(event)
  }

  initialize()
}
