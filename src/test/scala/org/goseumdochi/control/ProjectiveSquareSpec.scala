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

class ProjectiveSquareSpec extends AkkaSpecification("square-test.conf")
{
  "ControlActor with ProjectiveOrientationFsm with SquareFsm" should
  {
    "go round in squares" in new AkkaExample
    {
      val actuator = new TestActuator
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

      val backswingPos = PlanarPos(25.0, 20.0)
      val backswingTime = zeroTime + 25.seconds

      val alignedPos = PlanarPos(50.0, 20.0)
      val alignedTime = zeroTime + 30.seconds

      val startPos = PlanarPos(50.0, 10.0)
      val startTime = zeroTime + 35.seconds

      val nextPos = PlanarPos(60.0, 10.0)
      val nextTime = zeroTime + 40.seconds

      controlActor ! VisionActor.DimensionsKnownMsg(corner, initialTime)

      expectQuiet
      expectQuiet
      expectQuiet

      val backwardImpulse = actuator.retrieveImpulse.get
      backwardImpulse must be equalTo(PolarImpulse(0.2, 800.milliseconds, PI))
      actuator.reset

      controlActor ! VisionActor.HintBodyLocationMsg(initialPos, initialTime)

      expectQuiet

      controlActor ! MotionDetector.MotionDetectedMsg(initialPos, initialTime)
      controlActor ! BodyDetector.BodyDetectedMsg(initialPos, bodyFoundTime)

      expectQuiet

      val firstImpulse = actuator.retrieveImpulse.get
      firstImpulse must be equalTo(
        PolarImpulse(0.2, 1500.milliseconds, 0.0))
      actuator.reset

      controlActor !
        BodyDetector.BodyDetectedMsg(orientationPos, orientationTime)

      expectQuiet

      val secondImpulse = actuator.retrieveImpulse.get
      secondImpulse.theta must be closeTo(2.83 +/- 0.01)
      actuator.reset

      controlActor !
        BodyDetector.BodyDetectedMsg(backswingPos, backswingTime)

      expectQuiet

      val thirdImpulse = actuator.retrieveImpulse.get
      thirdImpulse.theta must be closeTo(-0.30 +/- 0.01)
      actuator.reset

      controlActor !
        BodyDetector.BodyDetectedMsg(alignedPos, alignedTime)

      expectQuiet

      val fourthImpulse = actuator.retrieveImpulse.get
      fourthImpulse.theta must be closeTo(-1.87 +/- 0.01)
      actuator.reset

      controlActor !
        BodyDetector.BodyDetectedMsg(startPos, startTime)

      expectQuiet

      actuator.retrieveImpulse must beEmpty

      controlActor !
        BodyDetector.BodyDetectedMsg(startPos, startTime)

      expectQuiet

      val fifthImpulse = actuator.retrieveImpulse.get
      fifthImpulse.theta must be closeTo(-0.30 +/- 0.01)
      actuator.reset

      controlActor !
        BodyDetector.BodyDetectedMsg(nextPos, nextTime)

      expectQuiet

      val sixthImpulse = actuator.retrieveImpulse.get
      sixthImpulse.theta must be closeTo(-1.87 +/- 0.01)
      actuator.reset
    }
  }
}
