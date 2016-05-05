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

object MotionDetector
{
  // result messages
  final case class MotionDetectedMsg(
    pos : PlanarPos, topLeft : RetinalPos, bottomRight : RetinalPos,
    eventTime : TimePoint)
      extends VisionActor.ObjDetectedMsg
  {
    override def renderOverlay(overlay : RetinalOverlay)
    {
      overlay.drawCircle(
        overlay.xform.worldToRetina(pos), 6, NamedColor.BLUE, 2)
      val topRight = RetinalPos(bottomRight.x, topLeft.y)
      val bottomLeft = RetinalPos(topLeft.x, bottomRight.y)
      overlay.drawLineSegment(topLeft, topRight, NamedColor.BLUE, 2)
      overlay.drawLineSegment(topRight, bottomRight, NamedColor.BLUE, 2)
      overlay.drawLineSegment(bottomRight, bottomLeft, NamedColor.BLUE, 2)
      overlay.drawLineSegment(bottomLeft, topLeft, NamedColor.BLUE, 2)
    }
  }
}

import MotionDetector._

abstract class MotionDetector(
  val settings : Settings, val xform : RetinalTransform,
  threshold : Int, under : Boolean = false)
    extends VisionAnalyzer
{
  override def analyzeFrame(
    img : IplImage, prevImg : IplImage, gray : IplImage, prevGray : IplImage,
    frameTime : TimePoint, hintBodyPos : Option[PlanarPos])
      : Iterable[MotionDetectedMsg] =
  {
    detectMotionMsg(prevGray, gray, frameTime)
  }

  private[vision] def detectMotion(beforeImg : IplImage, afterImg : IplImage)
      : Option[PlanarPos] =
  {
    detectMotionMsg(beforeImg, afterImg, TimePoint.ZERO).map(_.pos)
  }

  private[vision] def detectMotionMsg(
    beforeImg : IplImage, afterImg : IplImage, frameTime : TimePoint)
      : Option[MotionDetectedMsg] =
  {
    val diff = AbstractIplImage.create(
      beforeImg.width, beforeImg.height, IPL_DEPTH_8U, 1)

    val storage = AbstractCvMemStorage.create
    cvClearMemStorage(storage)
    cvAbsDiff(afterImg, beforeImg, diff)
    cvThreshold(diff, diff, 64, 255, CV_THRESH_BINARY)

    var contour = new CvSeq(null)
    cvFindContours(diff, storage, contour)

    try {
      while (contour != null && !contour.isNull) {
        if (contour.elem_size > 0) {
          val box = cvMinAreaRect2(contour, storage)
          if (box != null) {
            val size = box.size
            // FIXME:  return the largest object instead of the first
            // over the threshold, and if a body pos hint is available,
            // pick the one nearest/farthest from the body
            val detected = {
              if (under) {
                (size.width < threshold) && (size.height < threshold)
              } else {
                (size.width > threshold) && (size.height > threshold)
              }
            }
            if (detected) {
              val center = box.center
              val halfWidth = size.width / 2
              val halfHeight = size.height / 2
              val yOffset = {
                if (under) {
                  0
                } else {
                  // looking for something big:  use roughly the max y instead of
                  // the vertical center; this corresponds to the bottom
                  // after retinal flip
                  halfHeight
                }
              }
              return Some(MotionDetectedMsg(
                xform.retinaToWorld(RetinalPos(center.x, center.y + yOffset)),
                RetinalPos(center.x - halfWidth, center.y - halfHeight),
                RetinalPos(center.x + halfWidth, center.y + halfHeight),
                frameTime))
            }
          }
        }
        contour = contour.h_next()
      }
      return None
    } finally {
      diff.release
      storage.release
    }
  }
}

class CoarseMotionDetector(settings : Settings, xform : RetinalTransform)
    extends MotionDetector(
      settings, xform, settings.MotionDetection.coarseThreshold)

class FineMotionDetector(settings : Settings, xform : RetinalTransform)
    extends MotionDetector(
      settings, xform, settings.MotionDetection.fineThreshold)
