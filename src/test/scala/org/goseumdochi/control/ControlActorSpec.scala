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

package org.goseumdochi.control

import org.goseumdochi.common._
import org.goseumdochi.vision._
import org.goseumdochi.behavior._

import akka.actor._

import scala.math._
import MoreMath._

import scala.concurrent.duration._

import ControlActor._

class ControlActorSpec extends AkkaSpecification
{
  "ControlActor" should
  {
    "keep cool" in new AkkaExample
    {
      val actuator = new TestActuator(system)
      val controlActor = system.actorOf(
        Props(
          classOf[ControlActor],
          actuator,
          Props(classOf[NullActor]),
          false),
        "controlActor")

      val zeroTime = TimePoint.ZERO

      val initialPos = PlanarPos(25.0, 10.0)
      val initialTime = zeroTime + 1.second
      val corner = RetinalPos(100.0, 100.0)

      val bodyFoundTime = zeroTime + 10.seconds

      val orientationPos = PlanarPos(50.0, 20.0)
      val orientationTime = zeroTime + 17.seconds
      val visibleTime = zeroTime + 18.seconds
      val invisibleTime = zeroTime + 22.seconds

      controlActor ! VisionActor.DimensionsKnownMsg(corner, initialTime)

      val backwardImpulse = actuator.expectImpulse
      backwardImpulse must be equalTo(PolarImpulse(0.2, 800.milliseconds, PI))

      controlActor ! VisionActor.HintBodyLocationMsg(initialPos, initialTime)

      expectQuiet

      controlActor ! MotionDetector.MotionDetectedMsg(initialPos, initialTime)
      controlActor ! BodyDetector.BodyDetectedMsg(initialPos, bodyFoundTime)

      val orientationImpulse = actuator.expectImpulse
      orientationImpulse must be equalTo(
        PolarImpulse(0.2, 800.milliseconds, 0.0))

      controlActor !
        BodyDetector.BodyDetectedMsg(orientationPos, orientationTime)

      actuator.expectTwirlMsg.theta must be closeTo(-0.38 +/- 0.01)
      val centeringImpulse = actuator.expectImpulse
      centeringImpulse.speed must be closeTo(0.2 +/- 0.01)
      centeringImpulse.duration.toMillis must be equalTo 891
      centeringImpulse.theta must be closeTo(1.57 +/- 0.01)

      controlActor ! CheckVisibilityMsg(orientationTime)
      actuator.expectColor

      controlActor !
        BodyDetector.BodyDetectedMsg(orientationPos, visibleTime)

      controlActor ! CheckVisibilityMsg(visibleTime)
      actuator.expectColor

      controlActor ! CheckVisibilityMsg(invisibleTime)
      actuator.expectColor
      actuator.expectColor

      val panicImpulse = actuator.expectImpulse
      panicImpulse must be equalTo(centeringImpulse)

      expectQuiet
    }
  }
}
