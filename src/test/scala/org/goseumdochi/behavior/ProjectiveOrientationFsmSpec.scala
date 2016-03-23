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

import akka.actor._

import scala.concurrent.duration._

class ProjectiveOrientationFsmSpec extends AkkaSpecification
{
  "ProjectiveOrientationFsm" should
  {
    "calibrate alignment" in new AkkaExample
    {
      val fsm = system.actorOf(
        Props(classOf[ProjectiveOrientationFsm]))

      fsm ! ControlActor.CameraAcquiredMsg(TimePoint.ZERO)
      expectMsg(ControlActor.UseVisionAnalyzersMsg(Seq(
        "org.goseumdochi.vision.RoundBodyDetector"),
        TimePoint.ZERO))

      expectQuiescence

      val initialPos = PlanarPos(0, 0)
      val secondPos = PlanarPos(50, 50)
      val thirdPos = PlanarPos(150, 50)
      val finalPos = PlanarPos(150, 250)

      fsm ! ControlActor.BodyMovedMsg(initialPos, TimePoint.ZERO)

      val firstImpulse = expectMsgClass(
        classOf[ControlActor.ActuateImpulseMsg]).impulse
      firstImpulse.speed must be closeTo(0.2 +/- 0.01)
      firstImpulse.duration must be equalTo 1500.milliseconds
      firstImpulse.theta must be closeTo(0.0 +/- 0.01)

      fsm ! ControlActor.BodyMovedMsg(secondPos, TimePoint.ZERO)

      val secondImpulse = expectMsgClass(
        classOf[ControlActor.ActuateImpulseMsg]).impulse
      secondImpulse.speed must be closeTo(0.2 +/- 0.01)
      secondImpulse.duration must be equalTo 1500.milliseconds
      secondImpulse.theta must be closeTo(2.51 +/- 0.01)

      fsm ! ControlActor.BodyMovedMsg(thirdPos, TimePoint.ZERO)

      val thirdImpulse = expectMsgClass(
        classOf[ControlActor.ActuateImpulseMsg]).impulse
      thirdImpulse.speed must be closeTo(0.2 +/- 0.01)
      thirdImpulse.duration must be equalTo 1500.milliseconds
      thirdImpulse.theta must be closeTo(0.94 +/- 0.01)

      fsm ! ControlActor.BodyMovedMsg(finalPos, TimePoint.ZERO)

      val calibrationMsg = expectMsgClass(
        classOf[ControlActor.CalibratedMsg])
      val bodyMapping = calibrationMsg.bodyMapping
      bodyMapping.scale must be closeTo(333.3 +/- 0.1)
      bodyMapping.thetaOffset must be closeTo(-2.51 +/- 0.01)
      calibrationMsg.xform must beAnInstanceOf[ProjectiveRetinalTransform]
      val xform = calibrationMsg.xform.asInstanceOf[ProjectiveRetinalTransform]
      xform.worldOrigin must be equalTo finalPos
      xform.worldOrigin.x must be equalTo xform.retinaOrigin.x
      xform.worldOrigin.y must be equalTo xform.retinaOrigin.y
      xform.vSquash must be closeTo(2.0 +/- 0.1)
    }
  }
}
