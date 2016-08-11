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

package org.goseumdochi.sphero

import org.goseumdochi.common._
import org.goseumdochi.control._

import scala.concurrent.duration._

abstract class SpheroActuator extends Actuator
{
  protected def executeTemporaryMacro(builder : SpheroMacroBuilder)

  protected def executeCalibrate()

  override def actuateMotion(impulse : PolarImpulse)
  {
    actuateMotion(impulse, true)
  }

  private def actuateMotion(impulse : PolarImpulse, withBrackets : Boolean)
  {
    val indefinite = (impulse.duration == TimeSpans.INDEFINITE)
    val brackets = withBrackets && !indefinite
    val builder = new SpheroMacroBuilder
    if (brackets && (impulse.speed != 0)) {
      val spinImpulse = PolarImpulse(0.0, 500.milliseconds, impulse.theta)
      builder.roll(spinImpulse)
    }
    builder.roll(impulse)
    if (brackets) {
      builder.stop
      builder.waitUntilStopped(10.seconds)
    }
    if (indefinite) {
      builder.killMotor(false)
    }
    builder.end()

    executeTemporaryMacro(builder)
  }

  override def actuateTwirl(
    theta : Double, duration : TimeSpan, newHeading : Boolean)
  {
    if (newHeading) {
      val spin = PolarImpulse(0.0, duration, theta)
      actuateMotion(spin, false)
      Thread.sleep(duration.toMillis*2)
      executeCalibrate()
      return
    }

    val builder = new SpheroMacroBuilder
    builder.setBackLed(true)
    builder.twirl(theta, duration)
    builder.delay(duration)
    builder.setBackLed(false)
    builder.end()

    executeTemporaryMacro(builder)
  }
}
