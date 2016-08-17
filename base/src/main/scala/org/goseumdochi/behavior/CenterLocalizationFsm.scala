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

object CenterLocalizationFsm
{
  sealed trait State
  sealed trait Data

  // received messages
  // * ControlActor.CameraAcquiredMsg
  final case class BodyCenteredMsg(eventTime : TimePoint)
      extends VisionActor.ObjDetectedMsg

  // sent messages
  // * ControlActor.UseVisionAugmentersMsg
  // * VisionActor.HintBodyLocationMsg

  // states
  case object Blind extends State
  case object Waiting extends State
  case object Done extends State

  // data
  case object Empty extends Data
  final case class CenterPos(centerPos : PlanarPos) extends Data
}
import CenterLocalizationFsm._

class CenterLocalizationFsm()
    extends BehaviorFsm[State, Data]
{
  startWith(Blind, Empty)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg(bottomRight, eventTime), _) => {
      sender ! VisionActor.RequireLightMsg(NamedColor.WHITE, eventTime)
      sender ! ControlActor.UseVisionAugmentersMsg(Seq(
        classOf[CrosshairsGuideline].getName),
        eventTime)
      val xform = FlipRetinalTransform
      val centerPos = RetinalPos(bottomRight.x / 2, bottomRight.y / 2)
      goto(Waiting) using CenterPos(xform.retinaToWorld(centerPos))
    }
  }

  when(Waiting) {
    case Event(BodyCenteredMsg(eventTime), CenterPos(centerPos)) => {
      sender ! VisionActor.HintBodyLocationMsg(
        centerPos, eventTime)
      goto(Done)
    }
  }

  when(Done) {
    case _ => {
      stay
    }
  }

  initialize()
}
