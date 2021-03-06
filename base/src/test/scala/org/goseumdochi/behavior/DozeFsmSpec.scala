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

class DozeFsmSpec extends AkkaSpecification
{
  "DozeFsm" should
  {
    "catch forty winks" in new AkkaExample
    {
      val fsm = system.actorOf(
        Props(classOf[DozeFsm]))

      val initialPos = PlanarPos(0, 0)

      fsm ! ControlActor.CameraAcquiredMsg(DEFAULT_DIMS, TimePoint.ZERO)

      expectMsg(ControlActor.UseVisionAnalyzersMsg(
        Seq(),
        TimePoint.ZERO))

      fsm ! ControlActor.BodyMovedMsg(initialPos, TimePoint.ZERO)

      expectQuiescence
    }
  }
}
