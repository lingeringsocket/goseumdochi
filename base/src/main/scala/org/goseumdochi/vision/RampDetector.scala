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

import org.goseumdochi.common.MoreMath._

case class OrientedRamp(
  center : PlanarPos,
  entry : PlanarPos
)

object RampDetector
{
  // result messages
  final case class RampDetectedMsg(ramp : OrientedRamp, eventTime : TimePoint)
      extends VisionActor.ObjDetectedMsg
}

import RampDetector._

class RampDetector(val settings : Settings, val xform : RetinalTransform)
    extends VisionAnalyzer
{
  override def analyzeFrame(
    img : IplImage, prevImg : IplImage, gray : IplImage, prevGray : IplImage,
    frameTime : TimePoint, hintBodyPos : Option[PlanarPos])
      : Iterable[RampDetectedMsg] =
  {
    detectRamp(img).map(
      ramp => {
        cvCircle(
          img, OpenCvUtil.point(xform.worldToRetina(ramp.center)),
          2, NamedColor.GREEN, 6, CV_AA, 0)
        cvCircle(
          img, OpenCvUtil.point(xform.worldToRetina(ramp.entry)),
          2, NamedColor.BLUE, 6, CV_AA, 0)
        RampDetectedMsg(ramp, frameTime)
      }
    )
  }

  // from https://github.com/bytedeco/javacv/blob/master/samples/Square.java
  def detectRamp(img : IplImage) : Option[OrientedRamp] =
  {
    val sz = cvSize(img.width & -2, img.height & -2)
    val timg = img.clone
    val gray = AbstractIplImage.create(sz, 8, 1)
    val pyr = AbstractIplImage.create(cvSize(sz.width/2, sz.height/2), 8, 3)
    val tgray = AbstractIplImage.create(sz, 8, 1)
    val storage = AbstractCvMemStorage.create

    try {
      cvSetImageROI(timg, cvRect(0, 0, sz.width, sz.height))

      cvPyrDown(timg, pyr, 7)
      cvPyrUp(pyr, timg, 7)

      val THRESH = 50
      val N = 11

      for (c <- 0 until 3) {
        cvSetImageCOI(timg, c + 1)
        cvCopy(timg, tgray)

        for (l <- 0 until N) {
          if (l == 0) {
            cvCanny(tgray, gray, 0, THRESH, 5)
            cvDilate(gray, gray, null, 1)
          } else {
            cvThreshold(tgray, gray, (l + 1)*255/N, 255, CV_THRESH_BINARY)
          }

          var contours = new CvSeq
          cvFindContours(
            gray, storage, contours, Loader.sizeof(classOf[CvContour]),
            CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE, cvPoint(0,0))

          while ((contours != null) && !contours.isNull) {
            val result = cvApproxPoly(
              contours, Loader.sizeof(classOf[CvContour]), storage,
              CV_POLY_APPROX_DP, cvContourPerimeter(contours)*0.02, 0)
            if ((result.total == 4) &&
              (Math.abs(cvContourArea(result, CV_WHOLE_SEQ, 0)) > 1000) &&
              (cvCheckContourConvexity(result) != 0))
            {
              // drawRects(img, result)
              var s = 0.0
              var t = 0.0

              for (i <- 0 until 5) {
                if (i >= 2) {
                  val p0 = new CvPoint(cvGetSeqElem(result, i))
                  val p2 = new CvPoint(cvGetSeqElem(result, i-2))
                  val p1 = new CvPoint(cvGetSeqElem(result, i-1))
                  t = Math.abs(cornerCosine(p0, p2, p1))
                  if (t > s) {
                    s = t
                  }
                }
              }

              if (s < 0.3) {
                val p0 = OpenCvUtil.pos(new CvPoint(cvGetSeqElem(result, 0)))
                val p1 = OpenCvUtil.pos(new CvPoint(cvGetSeqElem(result, 1)))
                val p2 = OpenCvUtil.pos(new CvPoint(cvGetSeqElem(result, 2)))
                val side1 = polarMotion(p0, p1)
                val side2 = polarMotion(p1, p2)
                val major = Math.max(side1.distance, side2.distance)
                val minor = Math.min(side1.distance, side2.distance)
                val ratio = major/minor
                if ((ratio > 1.3) && (ratio < 1.33)) {
                  val center = midpoint(p0, p2)
                  // FIXME:  orientation (at least use hintBodyPos)
                  val entry = {
                    if (side1.distance < side2.distance) {
                      midpoint(p1, p2)
                    } else {
                      midpoint(p0, p1)
                    }
                  }
                  return Some(OrientedRamp(
                    xform.retinaToWorld(center), xform.retinaToWorld(entry)))
                }
              }
            }

            contours = contours.h_next
          }
        }
      }
    } finally {
      gray.release
      pyr.release
      tgray.release
      timg.release
      storage.release
    }
    return None
  }

  private def cornerCosine(
    pt1 : CvPoint, pt2 : CvPoint, pt0 : CvPoint) : Double =
  {
    val dx1 = pt1.x - pt0.x
    val dy1 = pt1.y - pt0.y
    val dx2 = pt2.x - pt0.x
    val dy2 = pt2.y - pt0.y

    (dx1*dx2 + dy1*dy2) /
      Math.sqrt((sqr(dx1) + sqr(dy1)) * (sqr(dx2) + sqr(dy2)) + 1e-10)
  }

  private def drawRects(img : IplImage, rects : CvSeq)
  {
    val slice = new CvSlice(rects)

    for (i <- 0 until rects.total by 4) {
      val rect = new CvPoint(4)
      val count = new IntPointer(1).put(4)
      cvCvtSeqToArray(rects, rect, slice.start_index(i).end_index(i + 4))

      cvPolyLine(
        img, rect.position(0), count, 1, 1, CV_RGB(0,255,0), 3, CV_AA, 0)
    }
  }
}
