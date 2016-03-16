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
import org.goseumdochi.perception._
import org.goseumdochi.vision._

import akka.actor._

abstract class ScriptedSpecification
    extends AkkaSpecification
{
  protected def runScript(
    actorSystem : ActorSystem,
    scriptResource : String) =
  {
    val path = getClass.getResource(scriptResource).getPath
    val seq = PerceptualLog.read(path)

    val actuator = new TestActuator(actorSystem, false)
    val controlActor = actorSystem.actorOf(
      Props(
        classOf[ControlActor],
        actuator,
        Props(classOf[NullActor]),
        false),
      ControlActor.CONTROL_ACTOR_NAME)
    seq.map(event => {
      event.msg match {
        case _ : ControlActor.ActuateImpulseMsg |
            _ : ControlActor.ActuateMoveMsg =>
          {
            // FIXME:  verify details for all actuate messages
            actuator.expectImpulse
          }
        case _ : ControlActor.ActuateTwirlMsg =>
          {
            actuator.expectTwirlMsg
          }
        case _ : ControlActor.ActuateLightMsg =>
          {
            actuator.expectColor
          }
        case _ : VisionActor.DimensionsKnownMsg |
            _ : VisionActor.HintBodyLocationMsg |
            _ : VisionActor.ObjDetectedMsg =>
          {
            controlActor ! event.msg
          }
        case _ =>
      }
    })
  }
}
