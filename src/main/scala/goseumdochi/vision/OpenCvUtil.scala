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

package goseumdochi.vision

import goseumdochi.common._

import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacv._

object OpenCvUtil
{
  private val converter = new OpenCVFrameConverter.ToIplImage

  def convert(img : IplImage) = converter.convert(img)

  def convert(frame : Frame) = converter.convert(frame)

  def grayscale(img : IplImage) =
  {
    val gray = cvCreateImage(cvGetSize(img), 8, 1)
    cvCvtColor(img, gray, CV_RGB2GRAY)
    cvSmooth(gray, gray, CV_GAUSSIAN, 3, 3, 0, 0)
    gray
  }

  def point(pos : PlanarPos) =
  {
    val point = new CvPoint2D32f
    point.x(pos.x.toFloat)
    point.y(pos.y.toFloat)
    cvPointFrom32f(point)
  }

  def pos(point : CvPoint) = PlanarPos(point.x, point.y)
}
