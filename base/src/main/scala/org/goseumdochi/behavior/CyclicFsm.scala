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

import akka.actor._

object CyclicFsm
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
  case object WaitingForStart extends State
  case object Moving extends State

  // data
  case object Empty extends Data
  case class Counter(c : Integer) extends Data
}
import CyclicFsm._

class CyclicFsm(impulses : IndexedSeq[PolarImpulse])
    extends BehaviorFsm[State, Data]
{
  startWith(Blind, Empty)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg(_, eventTime), _) => {
      sender ! ControlActor.UseVisionAnalyzersMsg(Seq(
        settings.BodyRecognition.className),
        eventTime)
      goto(WaitingForStart)
    }
  }

  when(WaitingForStart) {
    case Event(ControlActor.BodyMovedMsg(pos, eventTime), _) => {
      actuateMove(pos, 0, eventTime)
    }
  }

  when(Moving) {
    case Event(ControlActor.BodyMovedMsg(pos, eventTime), Counter(c)) => {
      actuateMove(pos, c, eventTime)
    }
  }

  whenUnhandled {
    case Event(msg : ControlActor.PanicAttackMsg, _) => {
      goto(Moving) using Counter(0)
    }
    case event => handleUnknown(event)
  }

  private def actuateMove(
    pos : PlanarPos, c : Integer, eventTime : TimePoint) =
  {
    val impulse = impulses(c)
    sender ! ControlActor.ActuateImpulseMsg(impulse, eventTime)
    var nextCounter = c + 1
    if (nextCounter >= impulses.size) {
      nextCounter = 0
    }
    goto(Moving) using Counter(nextCounter)
  }

  initialize()
}
