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

package org.goseumdochi.view

import org.goseumdochi.common._
import org.goseumdochi.perception._

import com.typesafe.config._

object ReplayMain extends App
{
  val config = ConfigFactory.load()
  val settings = new Settings(config, null)

  replay()

  def replay()
  {
    val path = args.headOption.getOrElse(
      getClass.getResource("/demo/quick.json").getPath)
    val seq = PerceptualLog.read(path)
    val view = settings.instantiateObject(settings.View.className).
      asInstanceOf[PerceptualProcessor]
    try {
      view.processHistory(seq)
    } finally {
      view.close
    }
  }
}
