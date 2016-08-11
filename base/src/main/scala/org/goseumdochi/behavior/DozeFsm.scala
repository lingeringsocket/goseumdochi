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

object DozeFsm
{
  sealed trait State
  sealed trait Data

  // states
  case object Dozing extends State

  // data
  case object Empty extends Data
}
import DozeFsm._

class DozeFsm()
    extends BehaviorFsm[State, Data]
{
  startWith(Dozing, Empty)

  when(Dozing) {
    case Event(ControlActor.CameraAcquiredMsg(_, eventTime), _) => {
      sender ! ControlActor.UseVisionAnalyzersMsg(
        Seq(),
        eventTime)
      stay
    }
    case Event(msg : EventMsg, _) => {
      stay
    }
  }

  whenUnhandled {
    case Event(msg : ControlActor.PanicAttackMsg, _) => {
      goto(Dozing)
    }
    case event => handleUnknown(event)
  }

  initialize()
}
