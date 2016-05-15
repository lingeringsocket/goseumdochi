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
import org.bytedeco.javacv._

object OpenCvUtil
{
  final val BGR_CHANNELS = Array("BLUE", "GREEN", "RED")

  def newConverter = new OpenCVFrameConverter.ToIplImage
  def newMatConverter = new OpenCVFrameConverter.ToMat

  def convert(img : IplImage) =
  {
    newConverter.convert(img)
  }

  def convert(frame : Frame) =
  {
    newConverter.convert(frame)
  }

  def grayscale(img : IplImage) =
  {
    val gray = AbstractIplImage.create(cvGetSize(img), 8, 1)
    cvCvtColor(img, gray, CV_RGB2GRAY)
    cvSmooth(gray, gray, CV_GAUSSIAN, 3, 3, 0, 0)
    gray
  }

  def point32f(pos : RetinalPos) =
    new CvPoint2D32f(pos.x.toFloat, pos.y.toFloat)

  def size32f(pos : RetinalPos) =
    new CvSize2D32f(pos.x.toFloat, pos.y.toFloat)

  def point(pos : RetinalPos) =
    cvPointFrom32f(point32f(pos))

  def pos(point : CvPoint) =
    RetinalPos(point.x, point.y)

  def pos(point : Point) =
    RetinalPos(point.x, point.y)

  def pos(point : CvPoint2D32f) =
    RetinalPos(point.x, point.y)
}
