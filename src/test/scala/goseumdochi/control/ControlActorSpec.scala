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

package goseumdochi.control

import goseumdochi.common._
import goseumdochi.vision._
import goseumdochi.behavior._

import akka.actor._

import ControlActor._

class ControlActorSpec extends AkkaSpecification("simulation.conf")
{
  "ControlActor" should
  {
    "handle panic" in new AkkaExample
    {
      val actuator = new TestActuator
      val controlActor = system.actorOf(
        Props(
          classOf[ControlActor],
          actuator,
          Props(classOf[NullActor]),
          Props(classOf[CalibrationFsm]),
          Props(classOf[DozeFsm]),
          false),
        "controlActor")

      val initialPos = PlanarPos(25.0, 10.0)
      val initialTime = 1000L
      val corner = PlanarPos(100.0, 100.0)

      val calibrationPos = PlanarPos(50.0, 20.0)
      val calibrationTime = 7000L
      val visibleTime = 8000L
      val invisibleTime = 12000L

      controlActor ! VisionActor.DimensionsKnownMsg(corner)

      controlActor ! BodyDetector.BodyDetectedMsg(initialPos, initialTime)

      expectQuiet

      val calibrationImpulse = actuator.retrieveImpulse().get
      calibrationImpulse must be equalTo(PolarImpulse(0.2, 0.8, 0.0))

      actuator.reset

      controlActor !
        BodyDetector.BodyDetectedMsg(calibrationPos, calibrationTime)

      controlActor ! CheckVisibilityMsg(calibrationTime)

      controlActor !
        BodyDetector.BodyDetectedMsg(calibrationPos, visibleTime)

      controlActor ! CheckVisibilityMsg(visibleTime)

      expectQuiet

      actuator.retrieveImpulse() must beEmpty

      actuator.reset

      controlActor ! CheckVisibilityMsg(invisibleTime)

      expectQuiet

      val panicImpulse = actuator.retrieveImpulse().get
      panicImpulse.speed  must be closeTo(0.2 +/- 0.01)
      panicImpulse.duration  must be closeTo(0.89 +/- 0.01)
      panicImpulse.theta  must be closeTo(1.19 +/- 0.01)

      expectQuiet
    }
  }
}
