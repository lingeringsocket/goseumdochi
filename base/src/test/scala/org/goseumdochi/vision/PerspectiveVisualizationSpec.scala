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

package org.goseumdochi.vision

import org.goseumdochi.common._
import org.goseumdochi.perception._
import org.goseumdochi.control._

import org.bytedeco.javacpp.opencv_imgcodecs._

class PerspectiveVisualizationSpec extends VisualizableSpecification
{
  "RestrictedPerspectiveTransform visualization" should
  {
    "deserialize and visualize" in
    {
      val path = getClass.getResource(
        "/unit/perspective-visualization.json").getPath
      val seq = PerceptualLog.read(path)
      val xform = seq.head.msg match {
        case msg : ControlActor.CalibratedMsg => {
          msg.xform must beAnInstanceOf[RestrictedPerspectiveTransform]
          msg.xform.asInstanceOf[RestrictedPerspectiveTransform]
        }
      }

      val worldPos = PlanarPos(0.0, 0.0)
      val retinalPos = xform.worldToRetina(worldPos)
      retinalPos must beRoughly(RetinalPos(497.7, 298.7))

      if (shouldVisualize) {
        val img = cvLoadImage("data/wallmount.jpg")
        xform.visualize(img)
        visualize(img)
      }

      xform.retinaToWorld(retinalPos) must beRoughly(worldPos)
    }
  }
}
