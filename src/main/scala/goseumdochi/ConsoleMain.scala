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

package goseumdochi

import collection.immutable._

object ConsoleMain extends App
{
  private val runMap = ListMap(
    "sphero" -> "goseumdochi.sphero.SpheroMain",
    "simulation" -> "goseumdochi.simulation.SimulationMain",
    "capture" -> "goseumdochi.vision.CaptureMain")

  if (args.isEmpty) {
    usage()
  } else {
    val key = args.head
    runMap.get(key) match {
      case Some(className) => {
        val subArgs = args.tail
        Class.forName(className).getMethod("main", args.getClass).invoke(
          null, subArgs)
      }
      case _ => usage()
    }
  }

  private def usage()
  {
    System.err.println("Usage:")
    System.err.println(
      "    sbt 'run sphero custom.conf'")
    runMap.foreach({
      case (command, className) => {
        System.err.println(
          "    sbt 'run " + command + "'")
        System.err.println(
          "    (equivalent to sbt 'run-main " + className + "')")
      }
    })
    System.err.println(
      "(Custom configuration, e.g. birdseye.conf, gets loaded from " +
        "src/main/resources)")
  }
}
