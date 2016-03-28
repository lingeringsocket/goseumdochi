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
import org.goseumdochi.behavior._

import scala.concurrent.duration._

class PerceptualLogSpec extends VisualizableSpecification
{
  "PerceptualLog" should
  {
    "read log" in
    {
      val path = getClass.getResource("/unit/perceptual.log").getPath
      val seq = PerceptualLog.read(path)
      seq.size must be equalTo(3)
      val first = seq.head
      first must be equalTo(
        PerceptualEvent(
          "first event",
          ControlActor.CONTROL_ACTOR_NAME,
          ControlActor.BEHAVIOR_ACTOR_NAME,
          ControlActor.BodyMovedMsg(
            PlanarPos(25.0, 10.0),
            TimePoint(TimeSpan(10, SECONDS)))))
      val second = seq(1)
      second.msg must beAnInstanceOf[ControlActor.CalibratedMsg]
      val msg = second.msg.asInstanceOf[ControlActor.CalibratedMsg]
      msg.xform must beAnInstanceOf[ProjectiveRetinalTransform]
      msg.xform.asInstanceOf[ProjectiveRetinalTransform].vSquash must
        be closeTo(0.5615731621894507 +/- 0.0001)
    }
  }
}