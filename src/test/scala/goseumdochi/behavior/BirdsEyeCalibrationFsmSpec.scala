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

import scala.math._

class BirdsEyeCalibrationFsmSpec extends AkkaSpecification
{
  "BirdsEyeCalibrationFsm" should
  {
    "calibrate body mapping" in new AkkaExample
    {
      val fsm = system.actorOf(
        Props(classOf[BirdsEyeCalibrationFsm]))

      fsm ! ControlActor.CameraAcquiredMsg(TimePoint.ZERO)
      expectMsg(VisionActor.ActivateAnalyzersMsg(Seq(
        "goseumdochi.vision.RoundBodyDetector",
        "goseumdochi.vision.FineMotionDetector")))

      expectQuiet

      val backwardImpulse = expectMsgClass(
        classOf[ControlActor.ActuateImpulseMsg]).impulse
      backwardImpulse.speed must be closeTo(0.2 +/- 0.01)
      backwardImpulse.duration.toMillis must be equalTo 800
      backwardImpulse.theta must be closeTo(Pi +/- 0.01)

      val initialPos = PlanarPos(0, 0)
      val finalPos = PlanarPos(100, 30)

      fsm ! MotionDetector.MotionDetectedMsg(initialPos, TimePoint.ZERO)

      expectMsg(VisionActor.HintBodyLocationMsg(initialPos, TimePoint.ZERO))

      fsm ! ControlActor.BodyMovedMsg(initialPos, TimePoint.ZERO)

      val forwardImpulse = expectMsgClass(
        classOf[ControlActor.ActuateImpulseMsg]).impulse
      forwardImpulse.speed must be closeTo(0.2 +/- 0.01)
      forwardImpulse.duration.toMillis must be equalTo 800
      forwardImpulse.theta must be closeTo(0.0 +/- 0.01)

      fsm ! ControlActor.BodyMovedMsg(finalPos, TimePoint.ZERO)

      val bodyMapping = expectMsgClass(
        classOf[ControlActor.CalibratedMsg]).bodyMapping
      bodyMapping.scale must be closeTo(652.5 +/- 0.1)
      bodyMapping.thetaOffset must be closeTo(0.29 +/- 0.01)
    }
  }
}
