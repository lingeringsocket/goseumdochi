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

import CalibrationFsm._

class CalibrationFsmSpec extends AkkaSpecification
{
  "CalibrationFsm" should
  {
    "calibrate body mapping" in new AkkaExample
    {
      val calibrationFsm = system.actorOf(
        Props(classOf[CalibrationFsm]))

      calibrationFsm ! ControlActor.CameraAcquiredMsg
      expectMsg(VisionActor.ActivateAnalyzersMsg(Seq(
        "goseumdochi.vision.RoundBodyDetector")))

      val initialPos = PlanarPos(0, 0)
      val finalPos = PlanarPos(100, 30)

      calibrationFsm ! ControlActor.BodyMovedMsg(initialPos, 0)

      val impulse = expectMsgClass(
        classOf[ControlActor.ActuateImpulseMsg]).impulse
      impulse.speed must be closeTo(0.2 +/- 0.01)
      impulse.duration must be closeTo(0.8 +/- 0.01)
      impulse.theta must be closeTo(0.0 +/- 0.01)

      calibrationFsm ! ControlActor.BodyMovedMsg(finalPos, 0)

      val bodyMapping = expectMsgClass(
        classOf[ControlActor.CalibratedMsg]).bodyMapping
      bodyMapping.scale must be closeTo(652.5 +/- 0.1)
      bodyMapping.thetaOffset must be closeTo(0.29 +/- 0.01)
    }
  }
}
