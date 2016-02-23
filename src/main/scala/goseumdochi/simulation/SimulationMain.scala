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

package goseumdochi.simulation

import goseumdochi.common._
import goseumdochi.control._
import goseumdochi.vision._
import goseumdochi.behavior._

import akka.actor._

import scala.concurrent._
import scala.concurrent.duration._

import com.typesafe.config._

object SimulationMain extends App
{
  run()

  private def run()
  {
    val videoStream = new PlaybackStream(
      Array(
        ("data/table1.jpg", 1000),
        ("data/table1.jpg", 1000),
        ("data/table1.jpg", 1000),
        ("data/table1.jpg", 1000),
        ("data/table2.jpg", 1000),
        ("data/table2.jpg", 1000),
        ("data/table2.jpg", 1000),
        ("data/table2.jpg", 1000),
        ("data/empty.jpg", 1000),
        ("data/empty.jpg", 1000),
        ("data/empty.jpg", 1000),
        ("data/empty.jpg", 1000),
        ("data/empty.jpg", 1000)),
      true)
    val actuator = NullActuator
    val props = Props(
      classOf[ControlActor],
      actuator,
      Props(classOf[VisionActor], videoStream),
      Props(classOf[CalibrationFsm]),
      Props(classOf[DozeFsm]),
      true)
    val simulationConfig = ConfigFactory.load("simulation.conf")
    val defaultConfig = ConfigFactory.load()
    val config = simulationConfig.withFallback(defaultConfig)
    val system = ActorSystem("SimulationActors", config)
    system.actorOf(props, "controlActor")
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
