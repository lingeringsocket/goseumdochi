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

trait BehaviorFsm[S, D] extends LoggingFSM[S, D]
{
  protected val settings = ActorSettings(context)

  protected def handleUnknown(event : Any) =
  {
    event match {
      case Event(e, s) => {
        log.warning("received unhandled request {} in state {}/{}",
          e, stateName, s)
      }
    }
    stay
  }

  protected def recordObservation(
    messageKey : String, eventTime : TimePoint,
    messageParams : Seq[Any] = Seq.empty)
  {
    // FIXME:  make this unconditional after updating
    // test scripts
    if (!settings.Test.active) {
      sender ! ControlActor.ObservationMsg(
        messageKey, eventTime, messageParams)
    }
  }
}
