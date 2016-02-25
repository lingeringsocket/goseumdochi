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

package goseumdochi.sphero

import goseumdochi.common._
import goseumdochi.control._
import goseumdochi.vision._
import goseumdochi.behavior._

import se.nicklasgavelin.bluetooth._
import se.nicklasgavelin.sphero._
import se.nicklasgavelin.sphero.RobotListener.EVENT_CODE
import se.nicklasgavelin.sphero.command._
import se.nicklasgavelin.sphero.response._
import se.nicklasgavelin.sphero.response.ResponseMessage.RESPONSE_CODE

import java.util._

import akka.actor._

import scala.concurrent._
import scala.concurrent.duration._

object SpheroMain extends App with BluetoothDiscoveryListener with RobotListener
{
  var systemOpt : Option[ActorSystem] = None

  run()

  private def run()
  {
    val system = ActorSystem("SpheroActors")
    systemOpt = Some(system)
    val settings = Settings(system)

    if (settings.Bluetooth.debug) {
      com.intel.bluetooth.DebugLog.setDebugEnabled(true)
    }

    val bt = new Bluetooth(this, Bluetooth.SERIAL_COM)
    val id = settings.Sphero.bluetoothId
    val btd = new BluetoothDevice(
      bt, "btspp://" + id + ":1;authenticate=true;encrypt=false;master=false")
    val r = new Robot(btd)
    r.addListener(this)
    if (r.connect(true)) {
      val videoStream = new RemoteVideoStream(settings)
      val actuator = new SpheroActuator(r)
      val props = Props(
        classOf[ControlActor],
        actuator,
        Props(classOf[VisionActor], videoStream),
        Props(classOf[CalibrationFsm]),
        Props(classOf[IntrusionDetectionFsm]),
        true)
      system.actorOf(props, "controlActor")
      Await.result(system.whenTerminated, Duration.Inf)
    }
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
      system => system.terminate
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
