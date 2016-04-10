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
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.helper.opencv_core._

// This transform implements a restricted subset of the general perspective
// transform.  It infers the transform parameters from a known
// point configuration, with the following assumptions:
// * input points are coplanar (all on the "floor")
// * camera is stationary and unrotated ("up" points away from the floor)
// * camera is above the floor looking downward at an arbitrary angle
//   (the extreme case of a worm's-eye view probably won't work well here)
// Should probably get rid of this and use OpenCV's findHomography instead.
case class RestrictedPerspectiveTransform(
  nearCenter : RetinalPos,
  farCenter : RetinalPos,
  distantCenter : RetinalPos,
  nearRight : RetinalPos,
  farRight : RetinalPos,
  worldDist : Double)
    extends RetinalTransform
{
  // from https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection
  private def intersection(
    lineA : (RetinalPos, RetinalPos),
    lineB : (RetinalPos, RetinalPos)) =
  {
    val x1 = lineA._1.x
    val y1 = lineA._1.y
    val x2 = lineA._2.x
    val y2 = lineA._2.y
    val x3 = lineB._1.x
    val y3 = lineB._1.y
    val x4 = lineB._2.x
    val y4 = lineB._2.y
    val denom = ((x1 - x2)*(y3 - y4)) - ((y1 - y2)*(x3 - x4))
    val n1 = x1*y2 - y1*x2
    val n2 = x3*y4 - y3*x4
    val x = (n1*(x3 - x4) - n2*(x1 - x2)) / denom
    val y = (n1*(y3 - y4) - n2*(y1 - y2)) / denom
    RetinalPos(x, y)
  }

  private val horizontalScale = worldDist / (nearRight.x - nearCenter.x)

  private val nearRightOrtho = RetinalPos(nearRight.x, nearCenter.y)

  private def yn = nearCenter.y
  private def yf = farCenter.y
  private def y2f = distantCenter.y
  private val paramC = 2*worldDist*(y2f - yf) / (yn + y2f - 2*yf)
  private val paramD = yf - (paramC / worldDist) * (yf - yn)
  private val paramE = -yn*paramC

  final val vanishingPoint : RetinalPos =
    intersection((nearCenter, farCenter), (nearRight, farRight))

  private def nearProjection(p : RetinalPos) : RetinalPos =
    intersection((nearCenter, nearRightOrtho), (vanishingPoint, p))

  override def retinaToWorld(p : RetinalPos) : PlanarPos =
  {
    val np = nearProjection(p)
    val x = horizontalScale * (np.x - nearCenter.x)
    val y = (paramE + paramC*p.y) / (p.y - paramD)
    PlanarPos(x, -y)
  }

  override def worldToRetina(p : PlanarPos) : RetinalPos =
  {
    val py = -p.y
    val y = (paramD*py + paramE) / (py - paramC)
    val yCenter = RetinalPos(nearCenter.x, y)
    val yRight = RetinalPos(nearRight.x, y)
    val xProj = RetinalPos(nearCenter.x + (p.x / horizontalScale), nearCenter.y)
    intersection((vanishingPoint, xProj),(yCenter, yRight))
  }

  def visualize(img : IplImage)
  {
    for (x <- -10 to 10) {
      for (y <- -10 to 10) {
        val wp = PlanarPos(x / 10.0, y / 10.0)
        val rp = worldToRetina(wp)
        val point = OpenCvUtil.point(rp)
        cvCircle(img, point, 2, AbstractCvScalar.RED, 6, CV_AA, 0)
      }
    }

    for (rp <- Array(nearCenter, farCenter,
      distantCenter, nearRight, farRight))
    {
      val point = OpenCvUtil.point(rp)
      cvCircle(img, point, 2, AbstractCvScalar.GREEN, 6, CV_AA, 0)
    }
  }
}

