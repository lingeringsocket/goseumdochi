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
import com.orbotix.common._
import com.orbotix.common.internal._
import com.orbotix.command._
import com.orbotix.`macro`._

trait AndroidSpheroContext
{
  def getRobot : Option[ConvenienceRobot]

  def getRotationCompensation : Double = 0.0
}

class AndroidSpheroActuator(
  context : AndroidSpheroContext)
    extends SpheroActuator with ResponseListener
{
  private var powerState = ""

  override def handleAsyncMessage(response : AsyncMessage, robot : Robot)
  {
  }

  override def handleStringResponse(response : String, robot : Robot)
  {
  }

  override def handleResponse(response : DeviceResponse, robot : Robot)
  {
    response match {
      case response : GetPowerStateResponse => {
        powerState = "{ " +
          ", state = " + response.getPowerState +
          ", voltage = " + response.getBatteryVoltage +
          ", charge count = " + response.getNumberOfCharges +
          ", time since last = " + response.getTimeSinceLastCharge +
          " }"
      }
      case _ => {
      }
    }
  }

  override def getPowerState : String =
  {
    if (powerState.isEmpty && !context.getRobot.isEmpty) {
      powerState = super.getPowerState
      context.getRobot.foreach(robot => {
        robot.addResponseListener(this)
        robot.sendCommand(new GetPowerStateCommand())
      })
    }
    powerState
  }

  override protected def executeTemporaryMacro(builder : SpheroMacroBuilder)
  {
    context.getRobot.foreach(robot => {
      val macroFlags = {
        if (builder.isKillMotorOnExit) {
          1.toByte
        } else {
          0.toByte
        }
      }
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

  override def setMotionTimeout(duration : TimeSpan)
  {
    context.getRobot.foreach(robot => {
      robot.sendCommand(new SetMotionTimeoutCommand(duration.toMillis.toInt))
    })
  }
}
