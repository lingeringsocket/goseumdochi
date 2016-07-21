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
import org.goseumdochi.common.MoreMath._

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._

import collection._
import util._

import BodyDetector._

class RoundBodyDetector(
  val settings : Settings, val xform : RetinalTransform)
    extends BodyDetector
{
  // use integers to allow for hash-based filtering
  // (which is tricky with doubles)
  case class RetinalCircle(
    centerX : Int,
    centerY : Int,
    radius : Int)

  private val sensitivity = conf.getInt("sensitivity")

  private var minRadius = settings.BodyRecognition.minRadius

  private var maxRadius = settings.BodyRecognition.maxRadius

  private val filteredCircles = new mutable.LinkedHashSet[RetinalCircle]

  override def analyzeFrame(
    imageDeck : ImageDeck, frameTime : TimePoint,
    hintBodyPos : Option[PlanarPos])
      : Iterable[BodyDetectedMsg] =
  {
    val currentGray = imageDeck.currentGray
    hintBodyPos match {
      case Some(hintPos) => {
        detectBodyMsg(
          imageDeck.currentBgr, currentGray, hintPos, frameTime)
      }
      case _ => {
        findBackgroundCircles(currentGray)
        Iterable.empty
      }
    }
  }

  private def blurCircle(c : RetinalCircle) =
    RetinalCircle(5*(c.centerX/5), 5*(c.centerY/5), 3*(c.radius/3))

  private[vision] def findBackgroundCircles(gray : IplImage)
  {
    filteredCircles ++= findCircles(gray).map(blurCircle)
  }

  private[vision] def detectBody(
    img : IplImage, gray : IplImage, hintBodyPos : PlanarPos)
      : Option[PlanarPos] =
    detectBodyMsg(img, gray, hintBodyPos, TimePoint.ZERO).map(_.pos)

  private[vision] def detectBodyMsg(
    img : IplImage, gray : IplImage, hintBodyPos : PlanarPos,
    frameTime : TimePoint)
      : Option[BodyDetectedMsg] =
  {
    val circles = findCircles(gray)

    val expectedPos = xform.worldToRetina(hintBodyPos)
    val expectedCircle = RetinalCircle(
      Math.round(expectedPos.x).toInt, Math.round(expectedPos.y).toInt, 0)

    def metric(c : RetinalCircle) =
    {
      val dx = c.centerX - expectedCircle.centerX
      val dy = c.centerY - expectedCircle.centerY
      sqr(dx) + sqr(dy)
    }
    val newCircles = circles.filterNot(
      c => filteredCircles.contains(blurCircle(c)))
    filteredCircles ++= newCircles.map(blurCircle)
    Try(newCircles.minBy(metric)) match {
      case Success(c : RetinalCircle) => {
        filteredCircles -= blurCircle(c)
        visualizeCircles(img, Iterable(c))
        minRadius = c.radius - 8
        if (minRadius < 1) {
          minRadius = 1
        }
        maxRadius = c.radius + 8
        val retinalPos = RetinalPos(c.centerX, c.centerY)
        if (xform.isValid(retinalPos)) {
          val msg =
            BodyDetectedMsg(
              xform.retinaToWorld(retinalPos),
              frameTime)
          newDebugger(img) { overlay =>
            msg.renderOverlay(overlay)
          }
          Some(msg)
        } else {
          None
        }
      }
      case _ => {
        None
      }
    }
  }

  private[vision] def findCircles(gray : IplImage) : Set[RetinalCircle] =
  {
    val mem = AbstractCvMemStorage.create
    try {
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

      (0 until circles.total).map(
        i => {
          val circle = new CvPoint3D32f(cvGetSeqElem(circles, i))
          RetinalCircle(
            Math.round(circle.x), Math.round(circle.y), Math.round(circle.z))
        }
      ).toSet
    } finally {
      mem.release
    }
  }

  private[vision] def visualizeCircles(
    img : IplImage, circles : Iterable[RetinalCircle])
  {
    newDebugger(img) { overlay =>
      circles.foreach(c => {
        val point = new CvPoint2D32f
        point.x(c.centerX.toFloat)
        point.y(c.centerY.toFloat)
        overlay.drawCircle(OpenCvUtil.pos(point), c.radius, NamedColor.RED, 6)
      })
    }
  }

  override def isLongLived() : Boolean = true
}
