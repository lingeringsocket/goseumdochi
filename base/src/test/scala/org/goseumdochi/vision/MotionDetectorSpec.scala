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

import org.bytedeco.javacpp.opencv_imgcodecs._

class MotionDetectorSpec extends VisualizableSpecification
{
  // motion detectors are mutable, so we need isolation
  isolated

  private val coarseGravityDetector = new CoarseGravityMotionDetector(
    settings, FlipRetinalTransform)

  private val coarseSizeDetector = new CoarseSizeMotionDetector(
    settings, FlipRetinalTransform)

  private val fineDetector = new FineSizeMotionDetector(
    settings, FlipRetinalTransform)

  private def loadImage(filename : String) =
    OpenCvUtil.grayscale(cvLoadImage(filename))

  private def detectMotion(
    motionDetector : MotionDetector,
    filename0 : String, filename1 : String, filename2 : String) =
  {
    val prevImg = loadImage(filename0)
    val beforeImg = loadImage(filename1)
    val afterImg = loadImage(filename2)

    motionDetector.detectMotion(prevImg, beforeImg)

    val coarseOpt = motionDetector.detectMotionMsg(
      beforeImg, afterImg, TimePoint.ZERO)
    val msg = coarseOpt.get
    postVisualize(motionDetector.getDebugImages)
    msg.pos
  }

  "MotionDetector" should
  {
    "detect nothing" in
    {
      val beforeImg = loadImage("data/baseline1.jpg")
      val afterImg = loadImage("data/baseline1.jpg")

      val coarseOpt = coarseSizeDetector.detectMotion(beforeImg, afterImg)
      coarseOpt must beEmpty

      val fineOpt = fineDetector.detectMotion(beforeImg, afterImg)
      fineOpt must beEmpty
    }

    "detect coarse motion" in
    {
      val pos = detectMotion(
        coarseSizeDetector,
        "data/baseline1.jpg", "data/baseline1.jpg", "data/intruder.jpg")
      pos.x must be closeTo(439.0 +/- 0.1)
      pos.y must be closeTo(-119.0 +/- 0.1)
    }

    "detect feet appearing" in
    {
      val pos = detectMotion(
        coarseGravityDetector,
        "data/walk1.jpg", "data/walk1.jpg", "data/walk2.jpg")
      pos.x must be closeTo(99.0 +/- 0.1)
      pos.y must be closeTo(-408.0 +/- 0.1)
    }

    "detect feet walking" in
    {
      val pos = detectMotion(
        coarseGravityDetector,
        "data/walk1.jpg", "data/walk2.jpg", "data/walk3.jpg")
      pos.x must be closeTo(546.0 +/- 0.1)
      pos.y must be closeTo(-239.0 +/- 0.1)
    }

    "detect feet turning" in
    {
      val pos = detectMotion(
        coarseGravityDetector,
        "data/walk2.jpg", "data/walk3.jpg", "data/walk4.jpg")
      pos.x must be closeTo(676.0 +/- 0.1)
      pos.y must be closeTo(-320.0 +/- 0.1)
    }

    "detect fine motion" in
    {
      val beforeImg = loadImage("data/room1.jpg")
      val afterImg = loadImage("data/room2.jpg")

      val coarseOpt = coarseSizeDetector.detectMotion(beforeImg, afterImg)
      coarseOpt must beEmpty

      val fineOpt = fineDetector.detectMotion(beforeImg, afterImg)
      fineOpt must not beEmpty

      val pos = fineOpt.get
      postVisualize(fineDetector.getDebugImages)

      pos.x must be closeTo(394.0 +/- 0.1)
      pos.y must be closeTo(-433.0 +/- 0.1)
    }
  }
}
