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

import org.bytedeco.javacpp.opencv_imgcodecs._

class MotionDetectorSpec extends VisualizableSpecification
{
  private val coarseDetector = new CoarseMotionDetector(
    settings, FlipRetinalTransform)

  private val fineDetector = new FineMotionDetector(
    settings, FlipRetinalTransform)

  private def loadImage(filename : String) =
    OpenCvUtil.grayscale(cvLoadImage(filename))

  "MotionDetector" should
  {
    "detect nothing" in
    {
      val beforeImg = loadImage("data/baseline1.jpg")
      val afterImg = loadImage("data/baseline1.jpg")

      val coarseOpt = coarseDetector.detectMotion(beforeImg, afterImg)
      coarseOpt must beEmpty

      val fineOpt = fineDetector.detectMotion(beforeImg, afterImg)
      fineOpt must beEmpty
    }

    "detect coarse motion" in
    {
      val beforeImg = loadImage("data/baseline1.jpg")
      val afterImg = loadImage("data/intruder.jpg")

      val coarseOpt = coarseDetector.detectMotion(beforeImg, afterImg)
      coarseOpt must not beEmpty

      val pos = coarseOpt.get
      visualize(afterImg, pos)

      pos.x must be closeTo(424.3 +/- 0.1)
      pos.y must be closeTo(-202.1 +/- 0.1)
    }

    "detect fine motion" in
    {
      val beforeImg = loadImage("data/room1.jpg")
      val afterImg = loadImage("data/room2.jpg")

      val coarseOpt = coarseDetector.detectMotion(beforeImg, afterImg)
      coarseOpt must beEmpty

      val fineOpt = fineDetector.detectMotion(beforeImg, afterImg)
      fineOpt must not beEmpty

      val pos = fineOpt.get
      visualize(afterImg, pos)

      pos.x must be closeTo(386.5 +/- 0.1)
      pos.y must be closeTo(-448.0 +/- 0.1)
    }
  }
}
