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

import goseumdochi.common._

import org.bytedeco.javacpp._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacv._

import collection._

import goseumdochi.common.MoreMath._

object BodyDetector
{
  // result messages
  final case class BodyDetectedMsg(pos : PlanarPos, eventTime : Long)
      extends VisionActor.ObjDetectedMsg
}

import BodyDetector._

abstract class BodyDetector(val settings : Settings)
    extends VisionAnalyzer
{
}

class RoundBodyDetector(settings : Settings)
    extends BodyDetector(settings)
{
  private val conf = settings.BodyRecognition.subConf

  private val sensitivity = conf.getInt("sensitivity")

  private val minRadius = conf.getInt("min-radius")

  private val maxRadius = conf.getInt("max-radius")

  override def analyzeFrame(
    img : IplImage, gray : IplImage, prevGray : IplImage, now : Long) =
  {
    detectBody(img, gray).map(
      pos => {
        BodyDetectedMsg(pos, now)
      }
    )
  }

  def detectBody(img : IplImage, gray : IplImage) : Option[PlanarPos] =
  {
    var result : Option[PlanarPos] = None

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
    if (circles.total > 0) {
      val circle = new CvPoint3D32f(cvGetSeqElem(circles, 0))
      val point = new CvPoint2D32f
      point.x(circle.x)
      point.y(circle.y)
      result = Some(PlanarPos(point.x, point.y))
      val center = cvPointFrom32f(point)
      val radius = Math.round(circle.z)
      cvCircle(img, center, radius, AbstractCvScalar.RED, 6, CV_AA, 0)
      cvCircle(img, center, 2, AbstractCvScalar.RED, 6, CV_AA, 0)
    }
    mem.release
    result
  }
}
