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

import MotionDetector._

import scala.concurrent.duration._

class IntrusionDetectionFsmSpec extends AkkaSpecification
{
  "IntrusionDetectionFsm" should
  {
    "maneuver to intruder" in new AkkaExample
    {

      val fsm = system.actorOf(
        Props(classOf[IntrusionDetectionFsm]))

      fsm ! ControlActor.CameraAcquiredMsg(TimePoint.ZERO)
      expectMsg(ControlActor.UseVisionAnalyzersMsg(Seq(
        "org.goseumdochi.vision.RoundBodyDetector",
        "org.goseumdochi.vision.CoarseMotionDetector"),
        TimePoint.ZERO))

      val initialPos = PlanarPos(0, 0)
      val intruderPos = PlanarPos(100, 100)

      fsm ! MotionDetectedMsg(intruderPos, TimePoint.ZERO)
      fsm ! ControlActor.BodyMovedMsg(initialPos, TimePoint.ZERO)

      val move1 = expectMsgClass(classOf[ControlActor.ActuateMoveMsg])
      move1.from must be equalTo(initialPos)
      move1.to must be equalTo(intruderPos)
      move1.speed must be equalTo(0.2)
      move1.extraTime must be equalTo 0.seconds

      val intermediatePos = PlanarPos(50.0, 50.0)
      fsm ! ControlActor.BodyMovedMsg(intermediatePos, TimePoint.ZERO)

      expectQuiet
    }
  }
}
