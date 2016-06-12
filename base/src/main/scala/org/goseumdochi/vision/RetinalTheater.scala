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

import org.bytedeco.javacv._
import org.bytedeco.javacpp.opencv_core._

trait RetinalTheaterListener
{
  def onTheaterClick(retinalPos : RetinalPos)
  {}

  def onTheaterClose()
  {}
}

trait RetinalTheater
{
  private var listener : Option[RetinalTheaterListener] = None

  def setListener(newListener : RetinalTheaterListener)
  {
    listener = Some(newListener)
  }

  def getListener = listener

  def imageToFrame(img : IplImage) = OpenCvUtil.convert(img)

  def display(frame : Frame, frameTime : TimePoint)

  def quit() {}
}

class TeeTheater(theaters : Iterable[RetinalTheater]) extends RetinalTheater
{
  override def setListener(listener : RetinalTheaterListener)
  {
    super.setListener(listener)
    theaters.foreach(_.setListener(listener))
  }

  override def imageToFrame(img : IplImage) =
    theaters.head.imageToFrame(img)

  override def display(frame : Frame, frameTime : TimePoint)
  {
    theaters.foreach(_.display(frame, frameTime))
  }

  override def quit()
  {
    theaters.foreach(_.quit)
  }
}
