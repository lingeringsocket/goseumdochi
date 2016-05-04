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
import org.goseumdochi.control._
import org.goseumdochi.vision._

import se.nicklasgavelin.sphero._

import akka.actor._

import com.typesafe.config._

object SpheroMain extends App with DesktopSpheroBase
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
      val actuator = new DesktopSpheroActuator(robotOpt.get)
      val props = Props(
        classOf[ControlActor],
        actuator,
        Props(classOf[VisionActor], retinalInput, new CanvasTheater))
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
      hardStop
    }
  }

  override protected def onDisconnect()
  {
    systemOpt.foreach(
      system => system.shutdown
    )
  }
}
