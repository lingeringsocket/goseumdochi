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

import se.nicklasgavelin.bluetooth._
import se.nicklasgavelin.sphero._
import se.nicklasgavelin.sphero.exception._
import se.nicklasgavelin.sphero.RobotListener.EVENT_CODE
import se.nicklasgavelin.sphero.command._
import se.nicklasgavelin.sphero.response._
import se.nicklasgavelin.sphero.response.ResponseMessage.RESPONSE_CODE

import java.util._

trait DesktopSpheroBase extends BluetoothDiscoveryListener with RobotListener
{
  protected def connectToRobot(id : String) : Robot =
  {
    if (id.isEmpty) {
      System.err.println(
        "Required property goseumdochi.sphero.bluetooth-id is not set.")
      System.err.println(
        "In base/src/main/resources, copy application.conf.template to ")
      System.err.println("application.conf, then edit for your configuration.")
      throw new Exception("Bluetooth not configured")
    }
    val bt = new Bluetooth(this, Bluetooth.SERIAL_COM)
    val btd = new BluetoothDevice(
      bt, "btspp://" + id + ":1;authenticate=true;encrypt=false;master=false")
    val robot = new Robot(btd)
    robot.addListener(this)

    while (true) {
      try {
        robot.connect(true)
        return robot
      } catch {
        case ex : RobotInitializeConnectionFailed => {
          println("RETRY...")
          Thread.sleep(200)
        }
      }
    }
    return robot
  }

  protected def hardStop()
  {
    // sphero+bluetooth shutdown may cause a hang, so use a hard stop
    System.runFinalization
    Runtime.getRuntime.halt(0)
  }

  override def deviceSearchStarted()
  {
    println("START DEVICE SEARCH")
  }

  override def deviceSearchFailed(error: Bluetooth.EVENT)
  {
    println("FAILED DEVICE SEARCH")
    println("MESSAGE = " + error.getErrorMessage)
  }

  override def deviceDiscovered(device : BluetoothDevice)
  {
    println("DISCOVERED DEVICE")
  }

  override def deviceSearchCompleted(devices : Collection[BluetoothDevice])
  {
    println("COMPLETED DEVICE SEARCH")
  }

  protected def onDisconnect()

  override def responseReceived(
    r : Robot, response : ResponseMessage, dc : CommandMessage)
  {
    if (response.getResponseCode == RESPONSE_CODE.CODE_ERROR_EXECUTE) {
      onDisconnect
    }
    if (response.getResponseCode == RESPONSE_CODE.CODE_ERROR_EXECUTE) {
      onDisconnect
    }
  }

  override def event(
    r : Robot, code : EVENT_CODE)
  {
    if (code == EVENT_CODE.CONNECTION_CLOSED_UNEXPECTED) {
      onDisconnect
    }
    if (code == EVENT_CODE.CONNECTION_FAILED) {
      onDisconnect
    }
  }

  override def informationResponseReceived(
    r : Robot, reponse : InformationResponseMessage )
  {
    println("INFORMATION RESPONSE RECEIVED")
  }
}
