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
import org.bytedeco.javacpp.opencv_video._

import scala.annotation._

import collection._

import BlobAnalysis._

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
  blobFilter : BlobFilter, blobSorter : BlobSorter)
    extends BlobAnalyzer
{
  private var lastRetinalPos : Option[RetinalPos] = None

  private var diffOpt : Option[IplImage] = None

  private val bgSubtractor = createBackgroundSubtractorMOG2(50, 130, false)

  override def analyzeFrame(
    imageDeck : ImageDeck, frameTime : TimePoint,
    hintBodyPos : Option[PlanarPos])
      : Iterable[MotionDetectedMsg] =
  {
    detectMotion(imageDeck.currentGray, frameTime)
  }

  private[vision] def detectMotion(
    img : IplImage, frameTime : TimePoint)
      : Option[MotionDetectedMsg] =
  {
    var first = diffOpt.isEmpty
    if (first) {
      diffOpt = Some(AbstractIplImage.create(
        img.width, img.height, IPL_DEPTH_8U, 1))
    }

    val diff = diffOpt.get

    cvZero(diff)
    bgSubtractor.apply(new Mat(img), new Mat(diff), -1)
    if (first) {
      return None
    }

    val rects = analyzeBlobs(diff, blobFilter, blobSorter)
    if (rects.isEmpty) {
      return None
    }
    val topTwo = rects.sortWith(blobSorter.compare(_, _) < 0).take(2)
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
    if (xform.isValid(retinalPos)) {
      val msg = MotionDetectedMsg(
        xform.retinaToWorld(retinalPos),
        OpenCvUtil.pos(farthest.tl),
        OpenCvUtil.pos(farthest.br),
        frameTime)
      newDebugger(img) { overlay =>
        msg.renderOverlay(overlay)
      }
      Some(msg)
    } else {
      None
    }
  }

  override def close()
  {
    diffOpt.foreach(_.release)
    diffOpt = None
  }
}

class CoarseGravityMotionDetector(settings : Settings, xform : RetinalTransform)
    extends MotionDetector(
      settings, xform,
      new IgnoreMedium(
        settings.MotionDetection.coarseThreshold,
        settings.MotionDetection.fineThreshold),
      GravitySorter)

class CoarseSizeMotionDetector(settings : Settings, xform : RetinalTransform)
    extends MotionDetector(
      settings, xform,
      new IgnoreSmall(settings.MotionDetection.coarseThreshold),
      BlobSizeSorter)

class FineGravityMotionDetector(settings : Settings, xform : RetinalTransform)
    extends MotionDetector(
      settings, xform,
      new IgnoreSmall(settings.MotionDetection.fineThreshold),
      GravitySorter)

class FineSizeMotionDetector(settings : Settings, xform : RetinalTransform)
    extends MotionDetector(
      settings, xform,
      new IgnoreSmall(settings.MotionDetection.fineThreshold),
      BlobSizeSorter)
