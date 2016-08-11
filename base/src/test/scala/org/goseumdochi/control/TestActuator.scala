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

import akka.actor._
import akka.testkit._

class TestActuator(system : ActorSystem, includeHeading : Boolean)
    extends Actuator
{
  val probe = TestProbe()(system)

  override def setMotionTimeout(duration : TimeSpan) {}

  override def actuateMotion(impulse : PolarImpulse)
  {
    probe.ref ! ControlActor.ActuateImpulseMsg(impulse, TimePoint.ZERO)
  }

  override def actuateLight(color : LightColor)
  {
    probe.ref ! ControlActor.ActuateLightMsg(color, TimePoint.ZERO)
  }

  override def actuateTwirl(
    theta : Double, duration : TimeSpan, newHeading : Boolean)
  {
    if (includeHeading || !newHeading) {
      probe.ref ! ControlActor.ActuateTwirlMsg(theta, duration, TimePoint.ZERO)
    }
  }

  def expectImpulse() =
    probe.expectMsgClass(classOf[ControlActor.ActuateImpulseMsg]).impulse

  def expectColor() =
    probe.expectMsgClass(classOf[ControlActor.ActuateLightMsg]).color

  def expectTwirlMsg() =
    probe.expectMsgClass(classOf[ControlActor.ActuateTwirlMsg])

  def expectImpulseMsg() =
    probe.expectMsgClass(classOf[ControlActor.ActuateImpulseMsg])
}
