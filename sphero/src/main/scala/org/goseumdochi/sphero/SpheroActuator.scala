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

import se.nicklasgavelin.sphero._
import se.nicklasgavelin.sphero.command._
import se.nicklasgavelin.util.Value

import scala.concurrent.duration._

class SpheroActuator(robot : Robot) extends Actuator
{
  private def executeTemporaryMacro(builder : SpheroMacroBuilder)
  {
    // kill motor on exit
    val macroFlags = SaveTemporaryMacroCommand.MacroFlagMotorControl
    robot.sendCommand(
      new SaveTemporaryMacroCommand(macroFlags, builder.getMacroBytes))
    robot.sendCommand(
      new RunMacroCommand(255))
  }

  override def actuateMotion(impulse : PolarImpulse)
  {
    actuateMotion(impulse, true)
  }

  private def actuateMotion(impulse : PolarImpulse, withStop : Boolean)
  {
    val stopImpulse = PolarImpulse(0.0, 1.seconds, impulse.theta)
    val builder = new SpheroMacroBuilder
    builder.roll(impulse)
    if (withStop) {
      builder.roll(stopImpulse)
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
      robot.sendCommand(new CalibrateCommand(0))
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

  override def actuateLight(color : java.awt.Color)
  {
    robot.sendCommand(
      new RGBLEDCommand(color))
  }
}
