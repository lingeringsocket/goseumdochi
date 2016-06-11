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

package org.goseumdochi.android

import android.graphics._
import android.view._

import java.util.concurrent._

import org.bytedeco.javacv._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._

import org.goseumdochi.common._
import org.goseumdochi.vision._

class AndroidTheater(
  view : View, outputQueue : ArrayBlockingQueue[Bitmap])
    extends RetinalTheater
{
  override def imageToFrame(img : IplImage) =
  {
    cvCvtColor(img, img, COLOR_BGRA2RGBA)
    super.imageToFrame(img)
  }

  override def display(frame : Frame, frameTime : TimePoint)
  {
    val converter = new AndroidFrameConverter
    val bitmap = converter.convert(frame)
    outputQueue.put(bitmap)
    view.postInvalidate
  }

  def getListener = listener
}
