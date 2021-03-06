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

import scala.concurrent.duration._

class RampJumperFsmSpec extends AkkaSpecification
{
  "RampJumperFsm" should
  {
    "launch" in new AkkaExample
    {
      val fsm = system.actorOf(
        Props(classOf[RampJumperFsm]))

      fsm ! ControlActor.CameraAcquiredMsg(DEFAULT_DIMS, TimePoint.ZERO)

      expectMsg(ControlActor.UseVisionAnalyzersMsg(Seq(
        "org.goseumdochi.vision.RoundBodyDetector",
        "org.goseumdochi.vision.RampDetector"),
        TimePoint.ZERO))

      val initialPos = PlanarPos(0, 0)
      val ramp = OrientedRamp(PlanarPos(100, 100), PlanarPos(100, 90))

      fsm ! RampDetector.RampDetectedMsg(ramp, TimePoint.ZERO)
      fsm ! ControlActor.BodyMovedMsg(initialPos, TimePoint.ZERO)

      val move1 = expectMsgClass(classOf[ControlActor.ActuateMoveMsg])
      move1.from must be equalTo(initialPos)
      move1.to.x must be closeTo(100.0 +/- 0.1)
      move1.to.y must be closeTo(30.0 +/- 0.1)
      move1.speed must be equalTo(0.5)
      move1.extraTime must be equalTo 0.seconds

      val intermediatePos = PlanarPos(80.0, 25.0)
      fsm ! ControlActor.BodyMovedMsg(intermediatePos, TimePoint.ZERO)

      val move2 = expectMsgClass(classOf[ControlActor.ActuateMoveMsg])
      move2.from must be equalTo(intermediatePos)
      move2.to.x must be closeTo(100.0 +/- 0.1)
      move2.to.y must be closeTo(30.0 +/- 0.1)
      move2.speed must be equalTo(0.5)
      move2.extraTime must be equalTo 200.milliseconds

      fsm ! ControlActor.BodyMovedMsg(move2.to, TimePoint.ZERO)

      val launch = expectMsgClass(classOf[ControlActor.ActuateMoveMsg])
      launch.from must be equalTo(move2.to)
      launch.to must be equalTo(ramp.center)
      launch.speed must be equalTo(1.0)
      launch.extraTime must be equalTo 1.second

      expectQuiescence
    }
  }
}
