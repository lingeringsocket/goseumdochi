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
import org.goseumdochi.control._

import scala.concurrent.duration._

import java.io._

import org.specs2.specification.core._

class PerceptualLogSpec extends VisualizableSpecification
{
  private val firstEvent =
    PerceptualEvent(
      "first event",
      ControlActor.CONTROL_ACTOR_NAME,
      ControlActor.BEHAVIOR_ACTOR_NAME,
      ControlActor.BodyMovedMsg(
        PlanarPos(25.0, 10.0),
        TimePoint(TimeSpan(10, SECONDS))))

  "PerceptualLog" should
  {
    "read log JSON" in
    {
      val path = resourcePath("/unit/perceptual.json")
      val seq = PerceptualLog.readJsonFile(path)
      seq.size must be equalTo(2)
      val first = seq.head
      first must be equalTo firstEvent
    }

    "write event JSON" in
    {
      val src = sourceFromPath(resourcePath("/unit/event.json"))
      val expected = src.getLines.mkString("\n")
      val result = PerceptualLog.toJsonString(firstEvent)
      result must be equalTo expected
    }

    "serialize and deserialize event" >> {
      Fragment.foreach(Seq(".ser", ".json")) { fileExt =>
        "using format " + fileExt ! {
          val file = File.createTempFile("event", fileExt)
          val filePath = file.getAbsolutePath
          PerceptualLog.serialize(filePath, Seq(firstEvent))
          val result = PerceptualLog.deserialize(filePath)
          file.delete
          result.size must be equalTo 1
          result.head must be equalTo firstEvent
        }
      }
    }
  }
}
