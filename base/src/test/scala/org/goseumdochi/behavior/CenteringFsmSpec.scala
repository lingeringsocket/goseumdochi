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

import scala.concurrent.duration._

class CenteringFsmSpec extends AkkaSpecification
{
  "CenteringFsm" should
  {
    "maneuver to center" in new AkkaExample
    {
      val fsm = system.actorOf(
        Props(classOf[CenteringFsm]))

      fsm ! ControlActor.CameraAcquiredMsg(
        RetinalPos(3000, 2000), TimePoint.ZERO)
      expectMsg(ControlActor.UseVisionAnalyzersMsg(Seq(
        "org.goseumdochi.vision.RoundBodyDetector"),
        TimePoint.ZERO))

      val centerPos = PlanarPos(1500, -1000)
      val initialPos = PlanarPos(0, 0)
      val secondPos = PlanarPos(100, -100)
      val thirdPos = PlanarPos(1499, -1001)

      val initialTime = TimePoint.ZERO
      val secondMotionTime = initialTime + 10.seconds
      val thirdMotionTime = initialTime + 20.seconds

      fsm ! ControlActor.BodyMovedMsg(initialPos, initialTime)

      val move1 = expectMsgClass(classOf[ControlActor.ActuateMoveMsg])
      move1.from must be equalTo(initialPos)
      move1.to must be equalTo(centerPos)
      move1.speed must be equalTo(0.5)
      move1.extraTime must be equalTo 0.seconds

      fsm ! ControlActor.BodyMovedMsg(secondPos, secondMotionTime)
      val move2 = expectMsgClass(classOf[ControlActor.ActuateMoveMsg])
      move2.from must be equalTo(secondPos)
      move2.to must be equalTo(centerPos)
      move2.speed must be equalTo(0.5)
      move2.extraTime must be equalTo 0.seconds

      fsm ! ControlActor.BodyMovedMsg(thirdPos, thirdMotionTime)

      // already close enough to center, so no motion expected
      expectQuiescence
    }
  }
}
