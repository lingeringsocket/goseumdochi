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

import scala.concurrent.duration._

object CenteringFsm
{
  sealed trait State
  sealed trait Data

  // received messages
  // * ControlActor.CameraAcquiredMsg
  // * ControlActor.BodyMovedMsg

  // sent messages
  // * ControlActor.UseVisionAnalyzersMsg
  // * ControlActor.ActuateMoveMsg

  // states
  case object Blind extends State
  case object Centering extends State

  // data
  case object Empty extends Data
  final case class Goal(
    pos : PlanarPos
  ) extends Data
}
import CenteringFsm._

class CenteringFsm()
    extends BehaviorFsm[State, Data]
{
  startWith(Blind, Empty)

  when(Blind) {
    case Event(msg : ControlActor.CameraAcquiredMsg, _) => {
      sender ! ControlActor.UseVisionAnalyzersMsg(Seq(
        settings.BodyRecognition.className),
        msg.eventTime)
      val bottomRight = msg.bottomRight
      val xform = FlipRetinalTransform
      val flipped = xform.retinaToWorld(bottomRight)
      val centerPos = PlanarPos(flipped.x / 2, flipped.y / 2)
      goto(Centering) using Goal(centerPos)
    }
  }

  when(Centering) {
    case Event(msg : ControlActor.BodyMovedMsg, Goal(goalPos)) => {
      val bodyPos = msg.pos
      val motion = polarMotion(bodyPos, goalPos)
      if (motion.distance > 100.0) {
        sender ! ControlActor.ActuateMoveMsg(
          bodyPos, goalPos, settings.Motor.defaultSpeed,
          0.seconds, msg.eventTime)
      }
      stay
    }
  }

  whenUnhandled {
    case Event(msg : ControlActor.PanicAttackMsg, _) => {
      goto(Centering)
    }
    case event => handleUnknown(event)
  }

  initialize()
}
