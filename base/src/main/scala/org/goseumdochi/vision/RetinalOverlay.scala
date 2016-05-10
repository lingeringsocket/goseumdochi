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

trait RetinalOverlay
{
  def xform : RetinalTransform

  def corner : RetinalPos

  def drawCircle(
    center : RetinalPos, radius : Int, color : LightColor, thickness : Int)

  def drawEllipse(
    center : RetinalPos, majorAxis : Double, minorAxis : Double,
    angle : Double, color : LightColor, thickness : Int)

  def drawLineSegment(
    end1 : RetinalPos, end2 : RetinalPos,
    color : LightColor, thickness : Int)

  def drawRectangle(
    topLeft : RetinalPos, bottomRight : RetinalPos,
    color : LightColor, thickness : Int)
  {
    val topRight = RetinalPos(bottomRight.x, topLeft.y)
    val bottomLeft = RetinalPos(topLeft.x, bottomRight.y)
    drawLineSegment(topLeft, topRight, color, thickness)
    drawLineSegment(topRight, bottomRight, color, thickness)
    drawLineSegment(bottomRight, bottomLeft, color, thickness)
    drawLineSegment(bottomLeft, topLeft, color, thickness)
  }
}

object NullRetinalOverlay extends RetinalOverlay
{
  override def xform = FlipRetinalTransform

  override def corner = RetinalPos(0, 0)

  override def drawCircle(
    center : RetinalPos, radius : Int, color : LightColor, thickness : Int)
  {}

  override def drawEllipse(
    center : RetinalPos, majorAxis : Double, minorAxis : Double,
    angle : Double, color : LightColor, thickness : Int)
  {}

  override def drawLineSegment(
    end1 : RetinalPos, end2 : RetinalPos,
    color : LightColor, thickness : Int)
  {}
}

class OpenCvRetinalOverlay(
  img : IplImage, retinalTransform : RetinalTransform, cornerInit : RetinalPos)
    extends RetinalOverlay
{
  override def xform = retinalTransform

  override def corner = cornerInit

  override def drawCircle(
    center : RetinalPos, radius : Int, color : LightColor, thickness : Int)
  {
    cvCircle(
      img, OpenCvUtil.point(center), radius, color, thickness, CV_AA, 0)
  }

  override def drawEllipse(
    center : RetinalPos, majorAxis : Double, minorAxis : Double,
    angle : Double, color : LightColor, thickness : Int)
  {
    val box = new CvBox2D(
      OpenCvUtil.point32f(center),
      OpenCvUtil.size32f(RetinalPos(majorAxis, minorAxis)),
      angle.toFloat)
    cvEllipseBox(
      img, box, color, thickness, CV_AA, 0)
  }

  override def drawLineSegment(
    end1 : RetinalPos, end2 : RetinalPos,
    color : LightColor, thickness : Int)
  {
    cvLine(
      img, OpenCvUtil.point(end1), OpenCvUtil.point(end2),
      color, thickness, CV_AA, 0)
  }
}
