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

object LightLocalizationFsm
{
  sealed trait State
  sealed trait Data

  // received messages
  // * ControlActor.CameraAcquiredMsg
  // * ControlActor.BodyMovedMsg

  // sent messages
  // * ControlActor.UseVisionAnalyzersMsg
  // * VisionActor.HintBodyLocationMsg

  // states
  case object Blind extends State
  case object WaitingForDark extends State
  case object FindingBody extends State
  case object Done extends State

  // data
  case object Empty extends Data
  final case class WithControl(
    controlActor : ActorRef,
    eventTime : TimePoint)
      extends Data
}
import LightLocalizationFsm._

class LightLocalizationFsm()
    extends BehaviorFsm[State, Data]
{
  private val quietPeriod = settings.Orientation.quietPeriod

  startWith(Blind, Empty)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg(_, eventTime), _) => {
      sender ! VisionActor.RequireLightMsg(NamedColor.BLACK, eventTime)
      goto(WaitingForDark) using WithControl(sender, eventTime + quietPeriod)
    }
  }

  when(WaitingForDark, stateTimeout = quietPeriod) {
    case Event(
      StateTimeout, WithControl(controlActor, eventTime)) =>
    {
      // in most cases this should be ColorfulBodyDetector
      controlActor ! ControlActor.UseVisionAnalyzersMsg(Seq(
        settings.BodyRecognition.className),
        eventTime)
      goto(FindingBody)
    }
    case _ => {
      stay
    }
  }

  when(FindingBody) {
    case Event(ControlActor.BodyMovedMsg(pos, eventTime), _) =>
    {
      sender ! VisionActor.HintBodyLocationMsg(pos, eventTime)
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
