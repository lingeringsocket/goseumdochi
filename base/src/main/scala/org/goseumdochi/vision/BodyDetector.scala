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

import collection._
import util._

object BodyDetector
{
  // result messages
  final case class BodyDetectedMsg(pos : PlanarPos, eventTime : TimePoint)
      extends VisionActor.ObjDetectedMsg
  {
    override def renderOverlay(overlay : RetinalOverlay)
    {
      overlay.drawCircle(
        overlay.xform.worldToRetina(pos), 6, NamedColor.RED, 2)
    }
  }
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
  private val random = scala.util.Random

  class BodyMotionDetector extends MotionDetector(
    settings, xform, settings.MotionDetection.bodyThreshold,
    MotionDetector.IGNORE_LARGE, MotionDetector.GravitySorter)

  private[vision] val motionDetector = new BodyMotionDetector

  override def analyzeFrame(
    imageDeck : ImageDeck, frameTime : TimePoint,
    hintBodyPos : Option[PlanarPos])
      : Iterable[VisionActor.AnalyzerResponseMsg] =
  {
    val randomColor = {
      if (random.nextBoolean) {
        NamedColor.WHITE
      } else {
        NamedColor.BLACK
      }
    }
    val light = Iterable(VisionActor.RequireLightMsg(randomColor, frameTime))
    val motion =
      motionDetector.detectMotion(
        imageDeck.previousGray, imageDeck.currentGray).map(
        pos => {
          val msg = BodyDetectedMsg(pos, frameTime)
          newDebugger(imageDeck.currentBgr) { overlay =>
            msg.renderOverlay(overlay)
          }
          msg
        }
      )
    light ++ motion
  }
}

// use integers to allow for hash-based filtering (which is tricky with doubles)
case class RetinalCircle(
  centerX : Int,
  centerY : Int,
  radius : Int)

class RoundBodyDetector(
  val settings : Settings, val xform : RetinalTransform)
    extends BodyDetector
{
  private val sensitivity = conf.getInt("sensitivity")

  private var minRadius = conf.getInt("min-radius")

  private var maxRadius = conf.getInt("max-radius")

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
        val msg =
          BodyDetectedMsg(
            xform.retinaToWorld(RetinalPos(c.centerX, c.centerY)),
            frameTime)
        newDebugger(img) { overlay =>
          msg.renderOverlay(overlay)
        }
        Some(msg)
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
}

class ColorfulBodyDetector(
  val settings : Settings, val xform : RetinalTransform)
    extends BodyDetector
{
  private var channels : Array[IplImage] = Array.empty

  private var totalDiffsOpt : Option[IplImage] = None

  private var hueOpt : Option[CvScalar] = None

  private var chosenColor : Option[LightColor] = None

  private var baselineMin = 0

  private var maxDiffCutoff = -1

  private var waitUntil = TimePoint.ZERO

  private def totalDiffs = totalDiffsOpt.get

  override def analyzeFrame(
    imageDeck : ImageDeck, frameTime : TimePoint,
    hintBodyPos : Option[PlanarPos])
      : Iterable[VisionActor.AnalyzerResponseMsg] =
  {
    val currentBgr = imageDeck.currentBgr
    val currentHsv = imageDeck.currentHsv
    chosenColor match {
      case Some(color) => {
        if (frameTime <= waitUntil) {
          None
        } else {
          compareColors(currentHsv, color)
          if (maxDiffCutoff < 0) {
            val newMin = computeMinDiff
            if (newMin + 3 > baselineMin) {
              return None
            }
            maxDiffCutoff = (0.95*baselineMin + 0.05*newMin).toInt
          }
          cvThreshold(
            totalDiffs, totalDiffs, maxDiffCutoff, 255,
            CV_THRESH_BINARY_INV)
          val msgOpt = locateBody(frameTime)
          msgOpt.map { msg =>
            newDebugger(currentBgr) { overlay =>
              msg.renderOverlay(overlay)
            }
          }
          msgOpt
        }
      }
      case _ => {
        val color = chooseColor(currentBgr)
        chosenColor = Some(color)
        waitUntil = frameTime + settings.Vision.sensorDelay
        compareColors(currentHsv, color)
        baselineMin = computeMinDiff
        Some(VisionActor.RequireLightMsg(color, frameTime))
      }
    }
  }

  private def locateBody(frameTime : TimePoint) =
  {
    newDebugger(totalDiffs)

    val imgToFrame = OpenCvUtil.newConverter
    val frameToMat = OpenCvUtil.newMatConverter
    val frame = imgToFrame.convert(totalDiffs)
    val mat = frameToMat.convert(frame)
    val locs = new Mat
    findNonZero(mat, locs)

    if (locs.empty) {
      None
    } else {
      // TODO:  use cvMoments instead
      val channels = new MatVector(2)
      split(locs, channels)
      val xChannel = channels.get(0)
      val yChannel = channels.get(1)

      val minVal = new Array[Double](1)
      val maxVal = new Array[Double](1)
      minMaxLoc(xChannel, minVal, maxVal, null, null, null)
      val xMin = minVal(0)
      val xMax = maxVal(0)
      minMaxLoc(yChannel, minVal, maxVal, null, null, null)
      val yMin = minVal(0)
      val yMax = maxVal(0)

      val retinalPos = RetinalPos((xMax + xMin) / 2, (yMax + yMin) / 2)
      val pos = xform.retinaToWorld(retinalPos)
      val msg = BodyDetectedMsg(pos, frameTime)
      Some(msg)
    }
  }

  override def close()
  {
    channels.foreach(_.release)
    channels = Array.empty
    totalDiffsOpt.foreach(_.release)
    totalDiffsOpt = None
  }

  private def compareColors(hsv : IplImage, color : LightColor)
  {
    val imgSize = cvGetSize(hsv)
    if (channels.isEmpty) {
      channels = OpenCvUtil.BGR_CHANNELS.map(
        c => AbstractIplImage.create(imgSize, 8, 1))
    }
    if (totalDiffsOpt.isEmpty) {
      totalDiffsOpt = Some(AbstractIplImage.create(imgSize, 8, 1))
    }

    if (hueOpt.isEmpty) {
      val onePixel = AbstractIplImage.create(1, 1, 8, 3)
      cvSet2D(onePixel, 0, 0, color)
      cvCvtColor(onePixel, onePixel, CV_BGR2HSV)
      val hsvTarget = cvGet2D(onePixel, 0, 0)
      onePixel.release
      hueOpt = Some(cvScalar(hsvTarget.getVal(0)))
    }
    val hue = hueOpt.get

    cvSplit(hsv, channels(0), channels(1), channels(2), null)
    cvThreshold(channels(1), channels(1), 60, 1, THRESH_BINARY)
    cvThreshold(channels(2), channels(2), 180, 1, THRESH_BINARY)
    cvMul(channels(0), channels(1), channels(0))
    cvMul(channels(0), channels(2), channels(0))
    cvAbsDiffS(channels(0), totalDiffs, hue)
  }

  private def computeMinDiff() =
  {
    val minVal = new Array[Double](1)
    cvMinMaxLoc(totalDiffs, minVal, null, null, null, null)
    minVal(0).toInt
  }

  private def chooseColor(img : IplImage) : LightColor =
  {
    // TODO:  choose contrasting color
    NamedColor.MAGENTA
  }
}

