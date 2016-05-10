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

import org.bytedeco.javacpp._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._

import scala.annotation._

import collection._

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
      overlay.drawRectangle(topLeft, bottomRight, NamedColor.BLUE, 2)
    }
  }

  type BlobFilter = (Rect, Int) => Boolean

  val IGNORE_SMALL : BlobFilter = {
    case (rect, threshold) => {
      (rect.size.width > threshold) && (rect.size.height > threshold)
    }
  }

  val IGNORE_LARGE : BlobFilter = {
    case (rect, threshold) => {
      (rect.size.width < threshold) && (rect.size.height < threshold)
    }
  }

  trait BlobSorter
  {
    def compare(rect1 : Rect, rect2 : Rect) : Int
    def getAnchor(rect : Rect) : RetinalPos
    def merge(rects : Seq[Rect]) : Seq[Rect]
  }

  object SizeSorter extends BlobSorter
  {
    override def compare(rect1 : Rect, rect2 : Rect) =
      (rect2.area - rect1.area).toInt

    override def getAnchor(rect : Rect) =
      RetinalPos((rect.tl.x + rect.br.x) / 2, (rect.tl.y + rect.br.y) / 2)

    override def merge(rects : Seq[Rect]) = rects
  }

  object GravitySorter extends BlobSorter
  {
    override def compare(rect1 : Rect, rect2 : Rect) =
      (getAnchor(rect2).y - getAnchor(rect1).y).toInt

    private def compareX(rect1 : Rect, rect2 : Rect) =
      (getAnchor(rect2).x - getAnchor(rect1).x).toInt

    override def getAnchor(rect : Rect) =
      RetinalPos((rect.tl.x + rect.br.x) / 2, rect.br.y)

    override def merge(rects : Seq[Rect]) =
      collapse(rects.toList.sortBy(_.tl.x))

    @tailrec private def collapse(rs: List[Rect], sep: List[Rect] = Nil)
        : List[Rect]=
    {
      rs match {
        case car :: cadr :: cddr => {
          if (cadr.tl.x > car.br.x) {
            collapse(cadr :: cddr, car :: sep)
          } else {
            val x1 = car.tl.x
            val y1 = Math.min(car.tl.y, cadr.tl.y)
            val x2 = Math.max(car.br.x, cadr.br.x)
            val y2 = Math.max(car.br.y, cadr.br.y)
            collapse(new Rect(x1, y1, x2 - x1, y2 - y1) :: cddr, sep)
          }
        }
        case _ => {
          (rs ::: sep).reverse
        }
      }
    }
  }
}

import MotionDetector._

abstract class MotionDetector(
  val settings : Settings, val xform : RetinalTransform,
  threshold : Int, blobFilter : BlobFilter, blobSorter : BlobSorter)
    extends VisionAnalyzer
{
  private var lastRetinalPos : Option[RetinalPos] = None

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
    cvThreshold(diff, diff, 32, 255, CV_THRESH_BINARY)

    var contour = new CvSeq(null)
    cvFindContours(diff, storage, contour, Loader.sizeof(classOf[CvContour]),
      CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE, cvPoint(0,0))

    val rawDebugger = newDebugger(diff)
    try {
      val rects = new mutable.ArrayBuffer[Rect]
      while (contour != null && !contour.isNull) {
        if (contour.elem_size > 0) {
          val box = cvMinAreaRect2(contour, storage)
          if (box != null) {
            val rect = box.asRotatedRect.boundingRect
            if (blobFilter(rect, threshold)) {
              rawDebugger { overlay =>
                overlay.drawEllipse(
                  OpenCvUtil.pos(box.center),
                  box.size.width,
                  box.size.height,
                  box.angle,
                  NamedColor.WHITE, 2)
              }
              rawDebugger { overlay =>
                overlay.drawRectangle(
                  OpenCvUtil.pos(rect.tl),
                  OpenCvUtil.pos(rect.br),
                  NamedColor.WHITE, 2)
              }
              rects += rect
            }
          }
        }
        contour = contour.h_next()
      }
      if (rects.isEmpty) {
        return None
      }
      val merged = blobSorter.merge(rects)
      val topTwo = merged.sortWith(blobSorter.compare(_, _) < 0).take(2)
      val farthest = lastRetinalPos match {
        case Some(lastAnchor) => {
          def anchorDistSqr(rect : Rect) = {
            val anchor = blobSorter.getAnchor(rect)
            sqr(anchor.x - lastAnchor.x) + sqr(anchor.y - lastAnchor.y)
          }
          topTwo.sortBy(anchorDistSqr).last
        }
        case _ => {
          topTwo.head
        }
      }
      val retinalPos = blobSorter.getAnchor(farthest)
      lastRetinalPos = Some(retinalPos)
      val msg = MotionDetectedMsg(
        xform.retinaToWorld(retinalPos),
        OpenCvUtil.pos(farthest.tl),
        OpenCvUtil.pos(farthest.br),
        frameTime)
      newDebugger(afterImg) { overlay =>
        msg.renderOverlay(overlay)
      }
      Some(msg)
    } finally {
      diff.release
      storage.release
    }
  }
}

class CoarseGravityMotionDetector(settings : Settings, xform : RetinalTransform)
    extends MotionDetector(
      settings, xform, settings.MotionDetection.coarseThreshold,
      IGNORE_SMALL, GravitySorter)

class CoarseSizeMotionDetector(settings : Settings, xform : RetinalTransform)
    extends MotionDetector(
      settings, xform, settings.MotionDetection.coarseThreshold,
      IGNORE_SMALL, SizeSorter)

class FineGravityMotionDetector(settings : Settings, xform : RetinalTransform)
    extends MotionDetector(
      settings, xform, settings.MotionDetection.fineThreshold,
      IGNORE_SMALL, GravitySorter)

class FineSizeMotionDetector(settings : Settings, xform : RetinalTransform)
    extends MotionDetector(
      settings, xform, settings.MotionDetection.fineThreshold,
      IGNORE_SMALL, SizeSorter)
