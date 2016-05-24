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

package org.goseumdochi.perception

import org.goseumdochi.common._
import org.goseumdochi.vision._

class PlanarPerception(settings : Settings)
{
  private var processor : PerceptualProcessor = NullPerceptualProcessor

  private def start()
  {
    if (settings.View.visualizeRetinal) {
      val view = settings.instantiateObject(settings.View.className).
        asInstanceOf[PerceptualProcessor]
      if (settings.Test.active) {
        processor = new PerceptualBuffer(view)
      } else {
        processor = view
      }
    }
    val logFile = settings.Perception.logFile
    if (!logFile.isEmpty) {
      processor = new PerceptualTee(Iterable(
        processor,
        new PerceptualLog(logFile)))
    }
  }

  def processEvent(event : PerceptualEvent)
  {
    event.msg match {
      case _ : VisionActor.DimensionsKnownMsg => {
        start()
      }
      case _ =>
    }
    processor.processEvent(event)
  }

  def end()
  {
    processor.close
  }
}
