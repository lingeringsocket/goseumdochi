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

import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core._

class RampDetectorSpec extends VisualizableSpecification
{
  private val rampDetector = new RampDetector(
    settings, FlipRetinalTransform)

  private def visualize(img : IplImage, ramp : OrientedRamp)
  {
    if (!shouldVisualize) {
      return
    }

    val center = OpenCvUtil.point(rampDetector.xform.worldToRetina(ramp.center))
    cvCircle(img, center, 2, AbstractCvScalar.BLUE, 6, CV_AA, 0)

    val entry = OpenCvUtil.point(rampDetector.xform.worldToRetina(ramp.entry))
    cvCircle(img, entry, 2, AbstractCvScalar.RED, 6, CV_AA, 0)

    visualize(img)
  }

  "RampDetector" should
  {
    "detect nothing" in
    {
      val img = cvLoadImage("data/baseline2.jpg")

      val rampOpt = rampDetector.detectRamp(img)
      rampOpt must beEmpty
    }

    "detect ramp" in
    {
      val img = cvLoadImage("data/ramp.jpg")

      val rampOpt = rampDetector.detectRamp(img)
      rampOpt must not beEmpty

      val ramp = rampOpt.get
      visualize(img, ramp)

      ramp.center.x must be closeTo(584.5 +/- 0.1)
      ramp.center.y must be closeTo(-313.0 +/- 0.1)

      ramp.entry.x must be closeTo(623.5 +/- 0.1)
      ramp.entry.y must be closeTo(-298.5 +/- 0.1)
    }
  }
}
