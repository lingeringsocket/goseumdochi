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

import org.bytedeco.javacpp._

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._

import scala.collection._

import BodyDetector._

class TemplateBodyDetector(
  val settings : Settings,
  val retinalTransformProvider : RetinalTransformProvider)
    extends BodyDetector
{
  private var maxRadius = settings.BodyRecognition.maxRadius

  private var swatchOpt : Option[IplImage] = None

  private var downSampleOpt : Option[IplImage] = None

  private var resultOpt : Option[IplImage] = None

  private[vision] def setMaxRadius(r : Int)
  {
    maxRadius = r
  }

  override def analyzeFrame(
    imageDeck : ImageDeck, frameTime : TimePoint,
    hintBodyPos : Option[PlanarPos])
      : Iterable[BodyDetectedMsg] =
  {
    hintBodyPos match {
      case Some(hintPos) => {
        detectBodyMsg(
          imageDeck.currentBgr, imageDeck.currentGray, hintPos, frameTime)
      }
      case _ => {
        Iterable.empty
      }
    }
  }

  override def close()
  {
    swatchOpt.foreach(_.release)
    swatchOpt = None
    resultOpt.foreach(_.release)
    resultOpt = None
    downSampleOpt.foreach(_.release)
    downSampleOpt = None
  }

  private def copySwatch(img : IplImage, center : RetinalPos)
  {
    val swatch = AbstractIplImage.create(maxRadius, maxRadius, IPL_DEPTH_8U, 1)
    swatchOpt = Some(swatch)
    val rect = new CvRect(
      ((center.x - maxRadius)*0.5).toInt,
      ((center.y - maxRadius)*0.5).toInt,
      maxRadius, maxRadius)
    cvSetImageROI(img, rect)
    cvCopy(img, swatch)
    cvResetImageROI(img)
    newDebugger(img) { overlay =>
      overlay.drawRectangle(
        RetinalPos(rect.x, rect.y),
        RetinalPos(rect.x + rect.width, rect.y + rect.height),
        NamedColor.BLACK, 4)
    }
    resultOpt = Some(
      AbstractIplImage.create(
        (img.width - maxRadius) + 1,
        (img.height - maxRadius) + 1,
        IPL_DEPTH_32F, 1))
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
    if (downSampleOpt.isEmpty) {
      val downSample = AbstractIplImage.create(
        gray.width / 2, gray.height / 2, IPL_DEPTH_8U, 1)
      downSampleOpt = Some(downSample)
    }
    val downSample = downSampleOpt.get
    cvPyrDown(gray, downSample)

    if (swatchOpt.isEmpty) {
      copySwatch(downSample, xform.worldToRetina(hintBodyPos))
    }
    val swatch = swatchOpt.get
    val result = resultOpt.get
    cvMatchTemplate(downSample, swatch, result, CV_TM_CCORR_NORMED)
    val minVal = new DoublePointer
    val maxVal = new DoublePointer(1L)
    val minLoc = new CvPoint
    val maxLoc = new CvPoint
    cvMinMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null)
    if (maxVal.get < 0.98) {
      None
    } else {
      val retinalPos = RetinalPos(
        maxLoc.x*2 + maxRadius, maxLoc.y*2 + maxRadius)
      val msg =
        BodyDetectedMsg(
          xform.retinaToWorld(retinalPos),
          frameTime)
      newDebugger(img) { overlay =>
        msg.renderOverlay(overlay)
      }
      Some(msg)
    }
  }

  override def isLongLived() : Boolean = true
}
