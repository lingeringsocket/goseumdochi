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

package org.goseumdochi.sphero.desktop

import org.goseumdochi.common._
import org.goseumdochi.control._
import org.goseumdochi.behavior._

import akka.actor._

class DesktopPacingSpec extends AkkaSpecification with DesktopSpheroBase
{
  var running = true

  "PacingFsm" should
  {
    "pace back and forth until robot gets tired" in new AkkaExample
    {
      skipped("requires real Sphero configuration")

      val id = settings.Sphero.bluetoothId
      val robot = connectToRobot(id)
      val actuator = new DesktopSpheroActuator(robot)
      val props = Props(classOf[PacingFsm])
      val actor = system.actorOf(props, "pacingActor")
      actor ! ControlActor.CameraAcquiredMsg(
        RetinalPos(100, 100), TimePoint.ZERO)
      expectMsgClass(classOf[ControlActor.UseVisionAnalyzersMsg])
      while (running) {
        actor ! ControlActor.BodyMovedMsg(
          PlanarPos(0, 0), TimePoint.ZERO)
        val impulse =
          expectMsgClass(classOf[ControlActor.ActuateImpulseMsg]).impulse
        actuator.actuateMotion(impulse)
        Thread.sleep(5000)
      }
      robot.disconnect
      running must beFalse
      hardStop
    }
  }

  override protected def onDisconnect()
  {
    running = false
  }
}

