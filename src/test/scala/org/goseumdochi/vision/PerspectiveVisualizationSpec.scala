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

import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core._

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
        for (x <- -10 to 10) {
          for (y <- -10 to 2) {
            val wp = PlanarPos(x / 10.0, y / 10.0)
            val rp = xform.worldToRetina(wp)
            val point = OpenCvUtil.point(rp)
            cvCircle(img, point, 2, AbstractCvScalar.RED, 6, CV_AA, 0)
          }
        }
        for (rp <- Array(xform.nearCenter, xform.farCenter,
          xform.distantCenter, xform.nearRight, xform.farRight))
        {
          val point = OpenCvUtil.point(rp)
          cvCircle(img, point, 2, AbstractCvScalar.GREEN, 6, CV_AA, 0)
        }
        visualize(img)
      }

      xform.retinaToWorld(retinalPos) must beRoughly(worldPos)
    }
  }
}
