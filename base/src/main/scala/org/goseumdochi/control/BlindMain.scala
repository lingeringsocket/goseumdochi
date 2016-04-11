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

package org.goseumdochi.control

import org.goseumdochi.common._

import akka.actor._

import com.typesafe.config._

object BlindMain extends App
{
  run()

  def start() =
  {
    val actuator = NullActuator
    val props = Props(
      classOf[ControlActor],
      actuator,
      Props(classOf[NullActor]),
      false)
    val config = ConfigFactory.load("orientation.conf")
    val system = ActorSystem("BlindActors", config)
    system.actorOf(props, ControlActor.CONTROL_ACTOR_NAME)
    system
  }

  private def run()
  {
    val system = start()
    system.awaitTermination
  }
}
