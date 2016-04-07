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

import org.goseumdochi.perception._
import org.goseumdochi.control._

import akka.actor._
import akka.testkit._

import org.specs2.execute._

class ScriptedBehaviorSpecification(confFile : String)
    extends AkkaSpecification(confFile)
{
  object ScriptedBehaviorExample
  {
    def apply(
      scriptResource : String,
      behaviorClass : Class[_],
      actorRoleName : String = ControlActor.BEHAVIOR_ACTOR_NAME,
      confFile : String = "") =
    {
      new ScriptedBehaviorExample(
        scriptResource, behaviorClass, actorRoleName, confFile)
      {
        processScript()
      }
    }
  }

  abstract class ScriptedBehaviorExample(
    scriptResource : String,
    behaviorClass : Class[_],
    actorRoleName : String,
    confFile : String)
      extends AkkaExample(confFile)
  {
    protected def processScript() =
    {
      val path = getClass.getResource(scriptResource).getPath
      val seq = PerceptualLog.read(path)

      val probe = TestProbe()(system)
      val behaviorActor = system.actorOf(
        Props(behaviorClass),
        actorRoleName)

      Result.unit {
        seq foreach {
          event => {
            if (event.receiver == actorRoleName) {
              behaviorActor.tell(event.msg, probe.ref)
            } else if (event.sender == actorRoleName) {
              probe.expectMsg(event.msg)
            }
          }
        }
      }
      expectQuiescence(probe)
    }
  }
}
