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

import collection._

import archery.RTree
import archery.Entry
import archery.Box

object BlobAnalysis
{
  trait BlobFilter
  {
    def apply(rect : Rect) : Boolean
  }

  trait BlobSorter
  {
    def compare(rect1 : Rect, rect2 : Rect) : Int
    def getAnchor(rect : Rect) : RetinalPos
    def merge(rects : Seq[Rect]) : Seq[Rect]
  }

  class IgnoreSmall(threshold : Int) extends BlobFilter
  {
    override def apply(rect : Rect) : Boolean =
      (rect.size.width > threshold) && (rect.size.height > threshold)
  }

  class IgnoreLarge(threshold : Int) extends BlobFilter
  {
    override def apply(rect : Rect) : Boolean =
      (rect.size.width < threshold) && (rect.size.height < threshold)
  }

  object KeepAll extends BlobFilter
  {
    override def apply(rect : Rect) : Boolean = true
  }

  def rectToString(r : Rect) =
  {
    "{(" + r.tl.x + ", " + r.tl.y + "), (" + r.br.x + ", " + r.br.y + ")}"
  }

}

import BlobAnalysis._

trait BlobSizeSorterTrait extends BlobSorter
{
  override def compare(rect1 : Rect, rect2 : Rect) =
    (rect2.area - rect1.area).toInt

  override def getAnchor(rect : Rect) =
    RetinalPos((rect.tl.x + rect.br.x) / 2, (rect.tl.y + rect.br.y) / 2)

  override def merge(rects : Seq[Rect]) = rects
}

object BlobSizeSorter extends BlobSizeSorterTrait

class BlobProximityMerger(proximity : Float) extends BlobSizeSorterTrait
{
  private def mergeTwoOverlap(r1 : Rect, r2 : Rect) : Rect =
    new Rect(
      new Point(Math.min(r1.tl.x, r2.tl.x), Math.min(r1.tl.y, r2.tl.y)),
      new Point(Math.max(r1.br.x, r2.br.x), Math.max(r1.br.y, r2.br.y)))

  private def mergeManyOverlap(rects : Iterable[Rect]) : Rect =
    rects.reduce(mergeTwoOverlap(_, _))

  override def merge(rects : Seq[Rect]) =
  {
    val entries = rects.map(
      r => Entry(Box(r.tl.x, r.tl.y, r.br.x, r.br.y), r))
    val rtree = RTree(entries:_*)
    val setDisjunction = DisjointSet(rects:_*)
    for (r <- rects) {
      val box = Box(
        r.tl.x - proximity, r.tl.y - proximity,
        r.br.x + proximity, r.br.y + proximity)
      for (e <- rtree.searchIntersection(box)) {
        val r2 = e.value
        setDisjunction.union(r, r2)
      }
    }
    val overlaps = rects.groupBy(r => setDisjunction.toNode(r).root)
    overlaps.toSeq.map {
      case (r, s) => {
        mergeManyOverlap(s)
      }
    }
  }
}

trait BlobAnalyzer extends VisionAnalyzer
{
  private val storage = AbstractCvMemStorage.create

  protected def analyzeBlobs(
    img : IplImage,
    blobFilter : BlobFilter,
    blobMerger : BlobSorter)
      : Seq[Rect] =
  {
    cvClearMemStorage(storage)
    var contour = new CvSeq(null)
    cvFindContours(img, storage, contour, Loader.sizeof(classOf[CvContour]),
      CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE, cvPoint(0,0))

    val rawDebugger = newDebugger(img)
    val rects = new mutable.ArrayBuffer[Rect]
    while (contour != null && !contour.isNull) {
      if (contour.elem_size > 0) {
        val cvRect = cvBoundingRect(contour)
        val rect = new Rect(cvRect.x, cvRect.y, cvRect.width, cvRect.height)
        if (blobFilter(rect)) {
          rawDebugger { overlay =>
            overlay.drawRectangle(
              OpenCvUtil.pos(rect.tl),
              OpenCvUtil.pos(rect.br),
              NamedColor.WHITE, 2)
          }
          rects += rect
        }
      }
      contour = contour.h_next()
    }
    if (rects.isEmpty) {
      return Seq.empty
    }
    blobMerger.merge(rects)
  }

  override def close()
  {
    storage.release
  }
}
