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
import org.bytedeco.javacpp.opencv_imgcodecs._

import java.io._

import collection._

import BlobAnalysis._
import BodyDetector._

class ColorfulBodyDetector(
  val settings : Settings, val xform : RetinalTransform)
    extends BodyDetector with BlobAnalyzer
{
  private val minDiameter = settings.BodyRecognition.minRadius*2

  private val debugDir = settings.Vision.debugDir

  // really just a hack for my old Galaxy Nexus, whose camera
  // sometimes creates a flickering ribbon of noise on one edge
  private val borderWidth = conf.getInt("border-width")

  private var debugging = !debugDir.isEmpty

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
          var setDiffCutoff = false
          if (maxDiffCutoff < 0) {
            if (debugging) {
              debugging = false
              val outFileName =
                new File(debugDir, "color_after.jpg").getAbsolutePath
              cvSaveImage(outFileName, currentBgr)
            }
            removeNoise
            val newMin = computeMinDiff
            if (newMin + 3 > baselineMin) {
              return None
            }
            setDiffCutoff = true
            maxDiffCutoff = (0.8*baselineMin + 0.2*newMin).toInt
            compareColors(currentHsv, color)
          }
          cvThreshold(
            totalDiffs, totalDiffs, maxDiffCutoff, 255,
            CV_THRESH_BINARY_INV)
          val msgOpt = locateBody(frameTime)
          if (setDiffCutoff && msgOpt.isEmpty) {
            // we got confused by the pretty lights...rollback and
            // try again next frame
            maxDiffCutoff = -1
          }
          msgOpt.map { msg =>
            newDebugger(currentBgr) { overlay =>
              msg.renderOverlay(overlay)
            }
          }
          msgOpt
        }
      }
      case _ => {
        if (debugging) {
          val dir = new File(debugDir)
          dir.mkdirs
          val outFileName =
            new File(dir, "color_before.jpg").getAbsolutePath
          cvSaveImage(outFileName, currentBgr)
        }
        val color = chooseColor(currentBgr)
        chosenColor = Some(color)
        waitUntil = frameTime + settings.Vision.sensorDelay
        compareColors(currentHsv, color)
        removeNoise
        baselineMin = computeMinDiff
        Some(VisionActor.RequireLightMsg(color, frameTime))
      }
    }
  }

  private def removeNoise()
  {
    cvSmooth(totalDiffs, totalDiffs, CV_MEDIAN, 5, 5, 0, 0)
    cvDilate(totalDiffs, totalDiffs, null, 3)
  }

  private def locateBody(frameTime : TimePoint) =
  {
    newDebugger(totalDiffs)

    val blobFilter = new IgnoreSmall(minDiameter)
    val blobSorter = new BlobProximityMerger(5)
    val rects = analyzeBlobs(
      totalDiffs, blobFilter, blobSorter)

    if (rects.isEmpty) {
      None
    } else {
      newDebugger(totalDiffs) { overlay =>
        rects.foreach(r => {
          overlay.drawRectangle(
            OpenCvUtil.pos(r.tl),
            OpenCvUtil.pos(r.br),
            NamedColor.WHITE, 2)
        })
      }
      val biggest = rects.sortWith(blobSorter.compare(_, _) < 0).head
      val retinalPos = blobSorter.getAnchor(biggest)
      if (xform.isValid(retinalPos)) {
        val pos = xform.retinaToWorld(retinalPos)
        val msg = BodyDetectedMsg(pos, frameTime)
        Some(msg)
      } else {
        None
      }
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
    if (borderWidth > 0) {
      val halfWidth = borderWidth / 2
      val topLeft = cvPoint(halfWidth, halfWidth)
      val bottomRight = cvPoint(
        totalDiffs.width - halfWidth, totalDiffs.height - halfWidth)
      cvRectangle(
        totalDiffs, topLeft, bottomRight, NamedColor.WHITE,
        borderWidth + 2, 4, 0)
    }
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
