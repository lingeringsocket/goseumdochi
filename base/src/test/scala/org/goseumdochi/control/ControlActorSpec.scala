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
import org.goseumdochi.common.MoreMath._
import org.goseumdochi.vision._

import akka.actor._
import akka.testkit._

import scala.concurrent.duration._

import ControlActor._
import ControlStatus._

class ControlActorSpec extends AkkaSpecification(
  "birdseye-orientation-test.conf")
{
  private val zeroTime = TimePoint.ZERO
  private val initialPos = PlanarPos(25.0, 10.0)
  private val initialTime = zeroTime + 1.second
  private val corner = RetinalPos(100.0, 100.0)
  private val bodyFoundTime = zeroTime + 10.seconds
  private val orientationPos = PlanarPos(50.0, 20.0)
  private val orientationTime = zeroTime + 17.seconds
  private val visibleTime = zeroTime + 18.seconds
  private val invisibleTime = zeroTime + 22.seconds
  private val retinalPos = RetinalPos(0, 0)

  private def expectStatusMsg(
    statusProbe : TestProbe, status : ControlStatus, eventTime : TimePoint)
  {
    statusProbe.expectMsg(StatusUpdateMsg(
      status, ControlActor.messageKeyFor(status), eventTime))
  }

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

      controlActor ! VisionActor.DimensionsKnownMsg(corner, initialTime)

      expectStatusMsg(statusProbe, LOCALIZING, initialTime)

      val backwardImpulse = actuator.expectImpulse
      backwardImpulse must be equalTo(PolarImpulse(0.5, 500.milliseconds, PI))

      controlActor ! VisionActor.HintBodyLocationMsg(initialPos, initialTime)

      expectQuiescence

      controlActor ! MotionDetector.MotionDetectedMsg(
        initialPos, retinalPos, retinalPos, initialTime)
      controlActor ! BodyDetector.BodyDetectedMsg(initialPos, bodyFoundTime)

      expectStatusMsg(statusProbe, ORIENTING, initialTime)

      val orientationImpulse = actuator.expectImpulse
      orientationImpulse must be equalTo(
        PolarImpulse(0.5, 500.milliseconds, 0.0))

      controlActor !
        BodyDetector.BodyDetectedMsg(orientationPos, orientationTime)

      actuator.expectTwirlMsg.theta must be closeTo(0.38 +/- 0.01)
      expectStatusMsg(statusProbe, ACTIVE, orientationTime)

      controlActor ! CheckVisibilityMsg(orientationTime)

      controlActor !
        BodyDetector.BodyDetectedMsg(orientationPos, visibleTime)

      controlActor ! CheckVisibilityMsg(visibleTime)

      controlActor ! CheckVisibilityMsg(invisibleTime)

      expectStatusMsg(statusProbe, PANIC, invisibleTime)

      val panicImpulse = actuator.expectImpulse
      panicImpulse.speed must be closeTo(0.5 +/- 0.01)
      panicImpulse.duration.toMillis must be equalTo 250
      panicImpulse.theta must be closeTo(3.14 +/- 0.01)

      expectQuiescence
    }

    "get lost" in new AkkaExample
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

      controlActor ! VisionActor.DimensionsKnownMsg(corner, initialTime)

      expectStatusMsg(statusProbe, LOCALIZING, initialTime)

      val backwardImpulse = actuator.expectImpulse
      backwardImpulse must be equalTo(PolarImpulse(0.5, 500.milliseconds, PI))

      controlActor ! VisionActor.HintBodyLocationMsg(initialPos, initialTime)

      expectQuiescence

      controlActor ! MotionDetector.MotionDetectedMsg(
        initialPos, retinalPos, retinalPos, initialTime)
      controlActor ! BodyDetector.BodyDetectedMsg(initialPos, bodyFoundTime)

      expectStatusMsg(statusProbe, ORIENTING, initialTime)

      val orientationImpulse = actuator.expectImpulse
      orientationImpulse must be equalTo(
        PolarImpulse(0.5, 500.milliseconds, 0.0))

      controlActor ! CheckVisibilityMsg(invisibleTime)
      expectStatusMsg(statusProbe, LOST, invisibleTime)

      expectQuiescence
    }
  }
}
