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

package goseumdochi.behavior

import goseumdochi.common._
import goseumdochi.control._
import goseumdochi.vision._

import akka.actor._

import IntrusionDetectionFsm._
import MotionDetector._
import goseumdochi.common.MoreMath._

class IntrusionDetectionFsmSpec extends AkkaSpecification
{
  "IntrusionDetectionFsm" should
  {
    "maneuver to intruder" in new AkkaExample
    {

      val fsm = system.actorOf(
        Props(classOf[IntrusionDetectionFsm]))

      fsm ! ControlActor.CameraAcquiredMsg
      expectMsg(VisionActor.ActivateAnalyzersMsg(Seq(
        "goseumdochi.vision.RoundBodyDetector",
        "goseumdochi.vision.MotionDetector")))

      val initialPos = PlanarPos(0, 0)
      val intruderPos = PlanarPos(100, 100)

      fsm ! MotionDetectedMsg(intruderPos, 0)
      fsm ! ControlActor.BodyMovedMsg(initialPos, 0)

      val move1 = expectMsgClass(classOf[ControlActor.ActuateMoveMsg])
      move1.from must be equalTo(initialPos)
      move1.to must be equalTo(intruderPos)
      move1.speed must be equalTo(0.2)
      move1.extraTime must be equalTo(0.2)

      val intermediatePos = PlanarPos(50.0, 50.0)
      fsm ! ControlActor.BodyMovedMsg(intermediatePos, 0)

      val move2 = expectMsgClass(classOf[ControlActor.ActuateMoveMsg])
      move2.from must be equalTo(intermediatePos)
      move2.to must be equalTo(intruderPos)
      move2.speed must be equalTo(0.2)
      move2.extraTime must be equalTo(0.2)

      fsm ! ControlActor.BodyMovedMsg(intruderPos, 0)

      expectQuiet
    }
  }
}
