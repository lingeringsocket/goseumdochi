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

class BodyDetectorSpec extends VisualizableSpecification
{
  // body detectors are mutable, so we need isolation
  isolated

  private val roundBodyDetector =
    new RoundBodyDetector(settings, FlipRetinalTransform)
  private val flashyBodyDetector =
    new FlashyBodyDetector(settings, FlipRetinalTransform)
  private val colorfulBodyDetector =
    new ColorfulBodyDetector(settings, FlipRetinalTransform)

  "BodyDetector" should
  {
    "detect nothing" in
    {
      val hintPos = PlanarPos(500, 500)
      val img = cvLoadImage("data/empty.jpg")
      val gray = OpenCvUtil.grayscale(img)
      val posOpt = roundBodyDetector.detectBody(img, gray, hintPos)
      posOpt must beEmpty
    }

    "detect round body with useless hint" in
    {
      val hintPos = PlanarPos(0, 0)
      val img = cvLoadImage("data/table1.jpg")
      val gray = OpenCvUtil.grayscale(img)
      val posOpt = roundBodyDetector.detectBody(img, gray, hintPos)
      posOpt must not beEmpty

      postVisualize(roundBodyDetector.getDebugImages)

      val pos = posOpt.get
      pos.x must be closeTo(563.0 +/- 0.1)
      pos.y must be closeTo(-451.0 +/- 0.1)
    }

    "detect round body with good hint" in
    {
      val img = cvLoadImage("data/baseline1.jpg")
      val gray = OpenCvUtil.grayscale(img)
      val hintPos = PlanarPos(500, -500)
      val posOpt = roundBodyDetector.detectBody(img, gray, hintPos)
      posOpt must not beEmpty

      postVisualize(roundBodyDetector.getDebugImages)

      val pos = posOpt.get
      pos.x must be closeTo(567.0 +/- 0.1)
      pos.y must be closeTo(-471.0 +/- 0.1)
    }

    "detect flashy body" in
    {
      val img1 = cvLoadImage("data/blinkoff.jpg")
      val img2 = cvLoadImage("data/blinkorange.jpg")
      val imageDeck = new ImageDeck
      imageDeck.cycle(img1)
      imageDeck.cycle(img2)
      val msgs = flashyBodyDetector.analyzeFrame(
        imageDeck, TimePoint.ZERO, None)
      msgs.size must be equalTo 2
      msgs.head must beAnInstanceOf[VisionActor.RequireLightMsg]
      msgs.last must beAnInstanceOf[BodyDetector.BodyDetectedMsg]

      postVisualize(
        flashyBodyDetector.motionDetector.getDebugImages ++
          flashyBodyDetector.getDebugImages)

      val pos = msgs.last.asInstanceOf[BodyDetector.BodyDetectedMsg].pos
      pos.x must be closeTo(268.0 +/- 0.1)
      pos.y must be closeTo(-372.0 +/- 0.1)
    }

    "find circles for round body background elimination" in
    {
      val img1 = cvLoadImage("data/circles1.jpg")
      val gray1 = OpenCvUtil.grayscale(img1)
      val circles = roundBodyDetector.findCircles(gray1)

      roundBodyDetector.visualizeCircles(img1, circles)
      postVisualize(roundBodyDetector.getDebugImages)

      circles.size must be equalTo 30
    }

    "detect round body after background elimination" in
    {
      val img1 = cvLoadImage("data/circles1.jpg")
      val gray1 = OpenCvUtil.grayscale(img1)
      val img2 = cvLoadImage("data/circles2.jpg")
      val gray2 = OpenCvUtil.grayscale(img2)
      val hintPos = PlanarPos(500, -500)

      roundBodyDetector.findBackgroundCircles(gray1)

      // img1 should show no body since all circles are considered background
      roundBodyDetector.detectBody(img1, gray1, hintPos) must beEmpty

      // but img2 should have something
      val posOpt = roundBodyDetector.detectBody(img2, gray2, hintPos)
      posOpt must not beEmpty

      val pos = posOpt.get

      val circles = roundBodyDetector.findCircles(gray2)
      roundBodyDetector.visualizeCircles(img2, circles)

      // even though we've already done background elimination,
      // multiple circles still show up due to sensitivity
      // in the algorithm, so from there we narrow it down
      // via hint position
      circles.size must be equalTo 6

      postVisualize(roundBodyDetector.getDebugImages)

      pos.x must be closeTo(363.0 +/- 0.1)
      pos.y must be closeTo(-539.0 +/- 0.1)
    }

    "detect colorful magenta body" in
    {
      val img1 = cvLoadImage("data/magenta_off.jpg")
      val img2 = cvLoadImage("data/magenta_on.jpg")

      val imageDeck = new ImageDeck

      // baseline:  let there be light
      imageDeck.cycle(img1)
      imageDeck.cycle(img1)
      val msgs1 = colorfulBodyDetector.analyzeFrame(
        imageDeck, TimePoint.ZERO, None)
      msgs1.size must be equalTo 1
      msgs1.head must be equalTo VisionActor.RequireLightMsg(
        NamedColor.MAGENTA, TimePoint.ZERO)

      // too early:  should be ignored
      imageDeck.cycle(img2)
      val msgs2 = colorfulBodyDetector.analyzeFrame(
        imageDeck, TimePoint.ZERO, None)
      msgs2 must beEmpty

      // no brightness change:  should be ignored
      imageDeck.cycle(img1)
      imageDeck.cycle(img1)
      val msgs3 = colorfulBodyDetector.analyzeFrame(
        imageDeck, TimePoint.ONE, None)
      msgs3 must beEmpty

      // should see the light now
      imageDeck.cycle(img2)
      val msgs4 = colorfulBodyDetector.analyzeFrame(
        imageDeck, TimePoint.ONE, None)

      msgs4.size must be equalTo 1
      msgs4.head must beAnInstanceOf[BodyDetector.BodyDetectedMsg]
      val pos = msgs4.head.asInstanceOf[BodyDetector.BodyDetectedMsg].pos

      postVisualize(colorfulBodyDetector.getDebugImages)

      pos.x must be closeTo(485.5 +/- 0.1)
      pos.y must be closeTo(-501.0 +/- 0.1)

      // now you see it, now you don't
      imageDeck.cycle(img1)
      imageDeck.cycle(img1)
      val msgs5 = colorfulBodyDetector.analyzeFrame(
        imageDeck, TimePoint.TEN, None)
      msgs5 must beEmpty
    }
  }
}
