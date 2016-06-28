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

import org.goseumdochi.common.MoreMath._
import org.goseumdochi.control._

object AboutFacePanicFsm
{
  sealed trait State
  sealed trait Data

  // states
  case object Calm extends State
  case object Panic extends State

  // data
  case object Empty extends Data
}
import AboutFacePanicFsm._

class AboutFacePanicFsm()
    extends BehaviorFsm[State, Data]
{
  startWith(Calm, Empty)

  when(Calm) {
    case Event(ControlActor.PanicAttackMsg(lastImpulse, eventTime), _) => {
      lastImpulse.foreach(forwardImpulse => {
        val reverseImpulse = forwardImpulse.copy(
          theta = normalizeRadians(forwardImpulse.theta + PI),
          duration = forwardImpulse.duration / 2)
        sender ! ControlActor.ActuateImpulseMsg(reverseImpulse, eventTime)
      })
      goto(Panic)
    }
  }

  when(Panic) {
    case Event(msg : ControlActor.BodyMovedMsg, _) => {
      sender ! ControlActor.PanicEndMsg(msg.eventTime)
      goto(Calm)
    }
  }

  whenUnhandled {
    case event => handleUnknown(event)
  }

  initialize()
}
