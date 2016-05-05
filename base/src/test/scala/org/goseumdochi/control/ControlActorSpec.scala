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

import akka.actor._
import akka.testkit._

import MoreMath._

import scala.concurrent.duration._

import ControlActor._
import ControlStatus._

class ControlActorSpec extends AkkaSpecification(
  "birdseye-orientation-test.conf")
{
  "ControlActor" should
  {
    "keep cool" in new AkkaExample
    {
      val actuator = new TestActuator(system, true)
      val statusProbe = TestProbe()(system)

      val controlActor = system.actorOf(
        Props(
          classOf[ControlActor],
          actuator,
          Props(classOf[NullActor])),
        ControlActor.CONTROL_ACTOR_NAME)
      ControlActor.addListener(controlActor, statusProbe.ref)

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

      statusProbe.expectMsg(StatusUpdate(LOCALIZING))

      val backwardImpulse = actuator.expectImpulse
      backwardImpulse must be equalTo(PolarImpulse(0.5, 500.milliseconds, PI))

      controlActor ! VisionActor.HintBodyLocationMsg(initialPos, initialTime)

      expectQuiescence

      val retinalPos = RetinalPos(0, 0)
      controlActor ! MotionDetector.MotionDetectedMsg(
        initialPos, retinalPos, retinalPos, initialTime)
      controlActor ! BodyDetector.BodyDetectedMsg(initialPos, bodyFoundTime)

      statusProbe.expectMsg(StatusUpdate(ORIENTING))

      val orientationImpulse = actuator.expectImpulse
      orientationImpulse must be equalTo(
        PolarImpulse(0.5, 500.milliseconds, 0.0))

      controlActor !
        BodyDetector.BodyDetectedMsg(orientationPos, orientationTime)

      actuator.expectTwirlMsg.theta must be closeTo(0.38 +/- 0.01)
      statusProbe.expectMsg(StatusUpdate(BEHAVING))

      controlActor ! CheckVisibilityMsg(orientationTime)

      controlActor !
        BodyDetector.BodyDetectedMsg(orientationPos, visibleTime)

      controlActor ! CheckVisibilityMsg(visibleTime)

      controlActor ! CheckVisibilityMsg(invisibleTime)

      statusProbe.expectMsg(StatusUpdate(PANIC))

      val panicImpulse = actuator.expectImpulse
      panicImpulse.speed must be closeTo(0.5 +/- 0.01)
      panicImpulse.duration.toMillis must be equalTo 1299
      panicImpulse.theta must be closeTo(-1.57 +/- 0.01)

      expectQuiescence
    }
  }
}
