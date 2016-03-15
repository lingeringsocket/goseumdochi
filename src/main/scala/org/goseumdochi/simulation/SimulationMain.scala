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

package org.goseumdochi.simulation

import org.goseumdochi.control._
import org.goseumdochi.vision._
import org.goseumdochi.behavior._

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
        ("data/table1.jpg", 1.second),
        ("data/table1.jpg", 1.second),
        ("data/table1.jpg", 1.second),
        ("data/table1.jpg", 1.second),
        ("data/table2.jpg", 1.second),
        ("data/table2.jpg", 1.second),
        ("data/table2.jpg", 1.second),
        ("data/table2.jpg", 1.second),
        ("data/empty.jpg", 1.second),
        ("data/empty.jpg", 1.second),
        ("data/empty.jpg", 1.second),
        ("data/empty.jpg", 1.second),
        ("data/empty.jpg", 1.second)),
      true)
    val actuator = NullActuator
    val props = Props(
      classOf[ControlActor],
      actuator,
      Props(classOf[VisionActor], videoStream),
      true)
    val config = ConfigFactory.load("simulation.conf")
    val system = ActorSystem("SimulationActors", config)
    system.actorOf(props, ControlActor.CONTROL_ACTOR_NAME)
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
