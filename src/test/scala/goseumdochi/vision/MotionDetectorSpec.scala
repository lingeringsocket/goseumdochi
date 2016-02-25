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

package goseumdochi.vision

import org.bytedeco.javacpp.opencv_highgui._

class MotionDetectorSpec extends VisualizableSpecification
{
  private val intrusionDetector = new MotionDetector(settings)

  private def loadImage(filename : String) =
    OpenCvUtil.grayscale(cvLoadImage(filename))

  "MotionDetector" should
  {
    "detect nothing" in
    {
      val beforeImg = loadImage("data/baseline1.jpg")
      val afterImg = loadImage("data/baseline2.jpg")

      val posOpt = intrusionDetector.detectIntruder(beforeImg, afterImg)
      posOpt must beEmpty
    }

    "detect motion" in
    {
      val beforeImg = loadImage("data/baseline1.jpg")
      val afterImg = loadImage("data/intruder.jpg")

      val posOpt = intrusionDetector.detectIntruder(beforeImg, afterImg)
      posOpt must not beEmpty

      val pos = posOpt.get
      visualize(afterImg, pos)

      pos.x must be closeTo(424.3 +/- 0.1)
      pos.y must be closeTo(152.0 +/- 0.1)
    }
  }
}
