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

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._

import MoreMath._

object BodyDetector
{
  // result messages
  final case class BodyDetectedMsg(pos : PlanarPos, eventTime : TimePoint)
      extends VisionActor.ObjDetectedMsg
}

import BodyDetector._

trait BodyDetector extends VisionAnalyzer
{
  protected val conf = settings.BodyRecognition.subConf
}

class FlashyBodyDetector(
  val settings : Settings, val xform : RetinalTransform)
    extends BodyDetector
{
  class BodyMotionDetector extends MotionDetector(
    settings, xform, settings.MotionDetection.bodyThreshold, true)

  private val motionDetector = new BodyMotionDetector

  override def analyzeFrame(
    img : IplImage, prevImg : IplImage, gray : IplImage, prevGray : IplImage,
    frameTime : TimePoint, hintBodyPos : Option[PlanarPos]) =
  {
    motionDetector.detectMotion(prevGray, gray).map(
      pos => {
        BodyDetectedMsg(pos, frameTime)
      }
    )
  }
}

class RoundBodyDetector(
  val settings : Settings, val xform : RetinalTransform)
    extends BodyDetector
{
  private val sensitivity = conf.getInt("sensitivity")

  private var minRadius = conf.getInt("min-radius")

  private var maxRadius = conf.getInt("max-radius")

  override def analyzeFrame(
    img : IplImage, prevImg : IplImage, gray : IplImage, prevGray : IplImage,
    frameTime : TimePoint, hintBodyPos : Option[PlanarPos]) =
  {
    hintBodyPos.flatMap(
      hintPos => detectBody(img, gray, hintPos).map(
        pos => {
          BodyDetectedMsg(pos, frameTime)
        }
      )
    )
  }

  def detectBody(img : IplImage, gray : IplImage,
    hintBodyPos : PlanarPos)
      : Option[PlanarPos] =
  {
    val mem = AbstractCvMemStorage.create
    val circles = cvHoughCircles(
      gray,
      mem,
      CV_HOUGH_GRADIENT,
      2,
      50,
      100,
      sensitivity,
      minRadius,
      maxRadius)

    var rMin = Double.MaxValue
    var closest : Option[CvPoint3D32f] = None

    for (i <- 0 until circles.total) {
      val circle = new CvPoint3D32f(cvGetSeqElem(circles, i))
      val dx = circle.x - hintBodyPos.x
      val dy = circle.y - hintBodyPos.y
      val r = sqr(dx) + sqr(dy)
      if (r < rMin) {
        rMin = r
        closest = Some(circle)
      }
    }

    if (closest.isEmpty) {
      return None
    }

    val circle = closest.get
    val point = new CvPoint2D32f
    point.x(circle.x)
    point.y(circle.y)
    val result = Some(RetinalPos(point.x, point.y))
    val center = cvPointFrom32f(point)
    val radius = Math.round(circle.z)
    minRadius = radius - 8
    if (minRadius < 1) {
      minRadius = 1
    }
    maxRadius = radius + 8
    cvCircle(img, center, radius, AbstractCvScalar.RED, 6, CV_AA, 0)

    mem.release
    result.map(xform.retinaToWorld(_))
  }
}
