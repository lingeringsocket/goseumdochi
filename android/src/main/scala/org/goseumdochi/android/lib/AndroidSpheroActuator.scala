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

package org.goseumdochi.android.lib

import org.goseumdochi.common._
import org.goseumdochi.sphero._

import com.orbotix._
import com.orbotix.command._
import com.orbotix.`macro`._

trait AndroidSpheroContext
{
  def getRobot : Option[ConvenienceRobot]

  def getRotationCompensation : Double = 0.0
}

class AndroidSpheroActuator(
  context : AndroidSpheroContext)
    extends SpheroActuator
{
  override protected def executeTemporaryMacro(builder : SpheroMacroBuilder)
  {
    context.getRobot.foreach(robot => {
      // kill motor on exit
      val macroFlags = 1.toByte
      robot.sendCommand(
        new SaveTemporaryMacroCommand(macroFlags, builder.getMacroBytes))
      robot.sendCommand(
        new RunMacroCommand(255.toByte))
    })
  }

  override protected def executeCalibrate()
  {
    context.getRobot.foreach(robot => {
      robot.sendCommand(new SetHeadingCommand(0))
    })
  }

  override def actuateLight(color : LightColor)
  {
    context.getRobot.foreach(robot => {
      robot.setLed(color.red.toFloat, color.green.toFloat, color.blue.toFloat)
    })
  }

  override def actuateMotion(impulse : PolarImpulse)
  {
    val rotated = PolarImpulse(
      impulse.speed, impulse.duration,
      impulse.theta - context.getRotationCompensation)
    super.actuateMotion(rotated)
  }
}
