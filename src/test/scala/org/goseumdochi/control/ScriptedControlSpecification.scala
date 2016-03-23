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

import org.specs2.execute._

abstract class ScriptedControlSpecification
    extends AkkaSpecification
{
  object ScriptedControlExample
  {
    def apply(scriptResource : String, confFile : String) =
    {
      new ScriptedControlExample(scriptResource, confFile)
      {
        processScript()
      }
    }
  }

  abstract class ScriptedControlExample(
    scriptResource : String,
    confFile : String)
      extends AkkaExample(confFile)
  {
    // FIXME:  for some reason everything goes haywire if I
    // try to declare actuator and controlActor here
    // instead of inside processScript :(

    protected def processScript() =
    {
      val path = getClass.getResource(scriptResource).getPath
      val seq = PerceptualLog.read(path)

      val actuator = new TestActuator(system, false)
      val controlActor = system.actorOf(
        Props(
          classOf[ControlActor],
          actuator,
          Props(classOf[NullActor]),
          false),
        ControlActor.CONTROL_ACTOR_NAME)
      Result.unit {
        seq foreach {
          event => processEvent(event, actuator, controlActor)
        }
      }
    }

    protected def processEvent(
      event : PerceptualEvent,
      actuator : TestActuator,
      controlActor : ActorRef) =
    {
      event.msg match {
        case m : ControlActor.ActuateImpulseMsg =>
          {
            actuator.expectImpulse must be equalTo m.impulse
          }
        case m : ControlActor.ActuateMoveMsg =>
          {
            // FIXME:  figure out how to map move to expected impulse
            actuator.expectImpulseMsg
          }
        case m : ControlActor.ActuateTwirlMsg =>
          {
            actuator.expectTwirlMsg must be equalTo m
          }
        case m : ControlActor.ActuateLightMsg =>
          {
            actuator.expectColor must be equalTo m.color
          }
        case _ : VisionActor.DimensionsKnownMsg |
            _ : VisionActor.HintBodyLocationMsg |
            _ : VisionActor.ObjDetectedMsg =>
          {
            controlActor ! event.msg
          }
        case _ =>
          "irrelevant message ignored by test"
      }
    }
  }
}
