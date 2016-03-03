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

import scala.math._
import MoreMath._

import se.nicklasgavelin.sphero._
import se.nicklasgavelin.sphero.command._
import se.nicklasgavelin.util.Value

class SpheroActuator(robot : Robot) extends Actuator
{
  override def actuateMotion(impulse : PolarImpulse)
  {
    // kill motor on exit
    val macroFlags = SaveTemporaryMacroCommand.MacroFlagMotorControl
    val macroDef = Array.ofDim[Byte](10)
    // first command:  ROLL2
    macroDef(0) = 0x1D.toByte
    val degrees = (360.0*normalizeRadiansPositive(impulse.theta) / TWO_PI).toInt
    val heading = (degrees % 360).toInt
    val velocity = Value.clamp(impulse.speed.toFloat, 0.0D, 1.0D).toFloat
    val millis = impulse.duration.toMillis.toInt
    macroDef(1) = (velocity * 255.0D).toInt.toByte
    macroDef(2) = (heading >> 8).toByte
    macroDef(3) = heading.toByte
    macroDef(4) = (millis >> 8).toByte
    macroDef(5) = millis.toByte
    // second command:  SET SPEED
    macroDef(6) = 0x25.toByte
    macroDef(7) = 0.toByte
    macroDef(8) = 0.toByte
    // third command:  END
    macroDef(9) = 0x1.toByte
    robot.sendCommand(
      new SaveTemporaryMacroCommand(macroFlags, macroDef))
    robot.sendCommand(
      new RunMacroCommand(255))
  }

  override def actuateTwirl(degrees : Int, duration : TimeSpan)
  {
    // kill motor on exit
    val macroFlags = SaveTemporaryMacroCommand.MacroFlagMotorControl
    val macroDef = Array.ofDim[Byte](15)
    // first command:  SET BACK LED on
    macroDef(0) = 0x9
    macroDef(1) = 0xFF.toByte
    macroDef(2) = 0
    // second command:  ROTATE OVER TIME
    val millis = duration.toMillis.toInt
    macroDef(3) = 0x1A
    macroDef(4) = (degrees >> 8).toByte
    macroDef(5) = degrees.toByte
    macroDef(6) = (millis >> 8).toByte
    macroDef(7) = millis.toByte
    // third command:  DELAY
    macroDef(8) = 0xB
    macroDef(9) = (millis >> 8).toByte
    macroDef(10) = millis.toByte
    // fourth command:  SET BACK LED off
    macroDef(11) = 0x9
    macroDef(12) = 0
    macroDef(13) = 0
    // fifth command macro:  END
    macroDef(14) = 0x1.toByte
    robot.sendCommand(
      new SaveTemporaryMacroCommand(macroFlags, macroDef))
    robot.sendCommand(
      new RunMacroCommand(255))
  }

  override def actuateLight(color : java.awt.Color)
  {
    robot.sendCommand(
      new RGBLEDCommand(color))
  }
}
