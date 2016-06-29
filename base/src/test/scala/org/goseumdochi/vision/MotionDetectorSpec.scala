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

  private def detectMotionOpt(
    motionDetector : MotionDetector,
    filename0 : String, filename1 : String, filename2 : String) =
  {
    postVisualize(motionDetector)

    val prevImg = loadImage(filename0)
    val beforeImg = loadImage(filename1)
    val afterImg = loadImage(filename2)

    motionDetector.detectMotion(prevImg, TimePoint.ZERO)
    motionDetector.detectMotion(beforeImg, TimePoint.ZERO)
    val msgOpt = motionDetector.detectMotion(
      afterImg, TimePoint.ZERO)
    msgOpt.map(_.pos)
  }

  private def detectMotion(
    motionDetector : MotionDetector,
    filename0 : String, filename1 : String, filename2 : String) =
  {
    detectMotionOpt(motionDetector, filename0, filename1, filename2).get
  }

  "MotionDetector" should
  {
    "detect nothing" in
    {
      val beforeImg = "data/baseline1.jpg"
      val afterImg = "data/baseline1.jpg"

      val coarseOpt = detectMotionOpt(
        coarseSizeDetector, beforeImg, beforeImg, afterImg)
      coarseOpt must beEmpty

      val fineOpt = detectMotionOpt(
        fineDetector, beforeImg, beforeImg, afterImg)
      fineOpt must beEmpty
    }

    "detect coarse motion" in
    {
      val pos = detectMotion(
        coarseSizeDetector,
        "data/baseline1.jpg", "data/baseline1.jpg", "data/intruder.jpg")
      pos.x must be closeTo(509.0 +/- 0.1)
      pos.y must be closeTo(-113.0 +/- 0.1)
    }

    "detect feet appearing" in
    {
      val pos = detectMotion(
        coarseGravityDetector,
        "data/walk1.jpg", "data/walk1.jpg", "data/walk2.jpg")
      pos.x must be closeTo(131.0 +/- 0.1)
      pos.y must be closeTo(-395.0 +/- 0.1)
    }

    "detect feet walking" in
    {
      val pos = detectMotion(
        coarseGravityDetector,
        "data/walk1.jpg", "data/walk2.jpg", "data/walk3.jpg")
      pos.x must be closeTo(551.0 +/- 0.1)
      pos.y must be closeTo(-207.0 +/- 0.1)
    }

    "detect feet turning" in
    {
      val pos = detectMotion(
        coarseGravityDetector,
        "data/walk2.jpg", "data/walk3.jpg", "data/walk4.jpg")
      pos.x must be closeTo(794.0 +/- 0.1)
      pos.y must be closeTo(-287.0 +/- 0.1)
    }

    "detect fine motion" in
    {
      postVisualize(coarseSizeDetector, fineDetector)

      val beforeImg = "data/room1.jpg"
      val afterImg = "data/room2.jpg"

      val coarseOpt = detectMotionOpt(
        coarseSizeDetector, beforeImg, beforeImg, afterImg)
      coarseOpt must beEmpty

      val fineOpt = detectMotionOpt(
        fineDetector, beforeImg, beforeImg, afterImg)
      fineOpt must not beEmpty

      val pos = fineOpt.get

      pos.x must be closeTo(394.0 +/- 0.1)
      pos.y must be closeTo(-433.0 +/- 0.1)
    }
  }
}
