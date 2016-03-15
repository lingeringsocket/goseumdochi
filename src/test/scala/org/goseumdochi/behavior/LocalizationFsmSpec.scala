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

import scala.math._
import MoreMath._

class LocalizationFsmSpec extends AkkaSpecification
{
  "LocalizationFsm" should
  {
    "find body" in new AkkaExample
    {
      val fsm = system.actorOf(
        Props(classOf[LocalizationFsm]))

      fsm ! ControlActor.CameraAcquiredMsg(TimePoint.ZERO)
      expectMsg(ControlActor.UseVisionAnalyzersMsg(Seq(
        "org.goseumdochi.vision.RoundBodyDetector",
        "org.goseumdochi.vision.FineMotionDetector"),
        TimePoint.ZERO))

      expectQuiet

      val backwardImpulse = expectMsgClass(
        classOf[ControlActor.ActuateImpulseMsg]).impulse
      backwardImpulse.speed must be closeTo(0.2 +/- 0.01)
      backwardImpulse.duration.toMillis must be equalTo 800
      backwardImpulse.theta must be closeTo(PI +/- 0.01)

      expectQuiet

      val forwardImpulse = expectMsgClass(
        classOf[ControlActor.ActuateImpulseMsg]).impulse
      forwardImpulse.speed must be closeTo(0.2 +/- 0.01)
      forwardImpulse.duration.toMillis must be equalTo 800
      forwardImpulse.theta must be closeTo(0.0 +/- 0.01)

      val finalPos = PlanarPos(100, 30)

      fsm ! MotionDetector.MotionDetectedMsg(finalPos, TimePoint.ZERO)

      expectMsg(VisionActor.HintBodyLocationMsg(finalPos, TimePoint.ZERO))

      expectQuiet
    }
  }
}
