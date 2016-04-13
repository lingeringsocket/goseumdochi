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
import org.goseumdochi.sphero._

import se.nicklasgavelin.sphero._
import se.nicklasgavelin.sphero.command._

class DesktopSpheroActuator(robot : Robot) extends SpheroActuator
{
  override protected def executeTemporaryMacro(builder : SpheroMacroBuilder)
  {
    // kill motor on exit
    val macroFlags = SaveTemporaryMacroCommand.MacroFlagMotorControl
    robot.sendCommand(
      new SaveTemporaryMacroCommand(macroFlags, builder.getMacroBytes))
    robot.sendCommand(
      new RunMacroCommand(255))
  }

  override protected def executeCalibrate()
  {
    robot.sendCommand(new CalibrateCommand(0))
  }

  override def actuateLight(color : LightColor)
  {
    robot.sendCommand(
      new RGBLEDCommand(color.red.toInt, color.green.toInt, color.blue.toInt))
  }
}
