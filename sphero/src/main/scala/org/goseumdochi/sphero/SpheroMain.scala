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
import org.goseumdochi.vision._

import se.nicklasgavelin.bluetooth._
import se.nicklasgavelin.sphero._
import se.nicklasgavelin.sphero.exception._
import se.nicklasgavelin.sphero.RobotListener.EVENT_CODE
import se.nicklasgavelin.sphero.command._
import se.nicklasgavelin.sphero.response._
import se.nicklasgavelin.sphero.response.ResponseMessage.RESPONSE_CODE

import java.util._

import akka.actor._

import com.typesafe.config._

object SpheroMain extends App with BluetoothDiscoveryListener with RobotListener
{
  var systemOpt : Option[ActorSystem] = None

  run()

  private def run()
  {
    val config = args.headOption match {
      case Some(configFile) => {
        println("Using custom configuration " + configFile)
        ConfigFactory.load(configFile)
      }
      case _ => {
        ConfigFactory.load()
      }
    }
    val system = ActorSystem("SpheroActors", config)
    systemOpt = Some(system)
    val settings = ActorSettings(system)

    val id = settings.Sphero.bluetoothId
    if (id.isEmpty) {
      System.err.println(
        "Required property goseumdochi.sphero.bluetooth-id is not set.")
      System.err.println(
        "In base/src/main/resources, copy application.conf.template to ")
      System.err.println("application.conf, then edit for your configuration.")
      system.shutdown
      return
    }
    var robotOpt : Option[Robot] = None
    try {
      if (settings.Bluetooth.debug) {
        com.intel.bluetooth.DebugLog.setDebugEnabled(true)
      }
      robotOpt = Some(connectToRobot(id))
      val retinalInput =
        settings.instantiateObject(settings.Vision.inputClass).
          asInstanceOf[RetinalInput]
      // pull and discard one frame as a test to make sure we have a
      // good connection
      retinalInput.beforeNext
      retinalInput.nextFrame
      retinalInput.afterNext
      val actuator = new SpheroActuator(robotOpt.get)
      val props = Props(
        classOf[ControlActor],
        actuator,
        Props(classOf[VisionActor], retinalInput, new CanvasTheater),
        true)
      val controlActor = system.actorOf(props, ControlActor.CONTROL_ACTOR_NAME)
    } catch {
      case ex : Throwable => {
        System.err.println("EXCEPTION:  " + ex)
        ex.printStackTrace
        system.shutdown
      }
    } finally {
      println("Close retina window to quit")
      system.awaitTermination
      systemOpt = None
      robotOpt.foreach(_.disconnect)
      println("Shutdown complete")
      // sphero+bluetooth shutdown may cause a hang, so use a hard stop
      System.runFinalization
      Runtime.getRuntime.halt(0)
    }
  }

  private def connectToRobot(id : String) : Robot =
  {
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

  private def stopActor()
  {
    systemOpt.foreach(
      system => system.shutdown
    )
  }

  override def responseReceived(
    r : Robot, response : ResponseMessage, dc : CommandMessage)
  {
    if (response.getResponseCode == RESPONSE_CODE.CODE_ERROR_EXECUTE) {
      stopActor()
    }
    if (response.getResponseCode == RESPONSE_CODE.CODE_ERROR_EXECUTE) {
      stopActor()
    }
  }

  override def event(
    r : Robot, code : EVENT_CODE)
  {
    if (code == EVENT_CODE.CONNECTION_CLOSED_UNEXPECTED) {
      stopActor()
    }
    if (code == EVENT_CODE.CONNECTION_FAILED) {
      stopActor()
    }
  }

  override def informationResponseReceived(
    r : Robot, reponse : InformationResponseMessage )
  {
    println("INFORMATION RESPONSE RECEIVED")
  }
}
