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

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.helper.opencv_core._

import collection._

class ImageDeck
{
  private val NO_CONVERSION = 0

  private val buf = new mutable.ArrayBuffer[mutable.Map[Int, IplImage]]

  private def lookup(
    map : mutable.Map[Int, IplImage], conversion : Int, nChannels : Int) =
  {
    map.get(conversion).getOrElse {
      val bgr = map(NO_CONVERSION)
      val img = AbstractIplImage.create(cvGetSize(bgr), 8, nChannels)
      cvCvtColor(bgr, img, conversion)
      if (conversion == CV_BGR2GRAY) {
        cvSmooth(img, img, CV_GAUSSIAN, 3, 3, 0, 0)
      }
      map.put(conversion, img)
      img
    }
  }

  def current(conversion : Int, nChannels : Int) : IplImage =
    lookup(buf.last, conversion, nChannels)

  def previous(conversion : Int, nChannels : Int) : IplImage =
    lookup(buf.head, conversion, nChannels)

  def currentBgr = current(NO_CONVERSION, 3)

  def previousBgr = previous(NO_CONVERSION, 3)

  def currentHsv = current(CV_BGR2HSV, 3)

  def previousHsv = previous(CV_BGR2HSV, 3)

  def currentGray = current(CV_BGR2GRAY, 1)

  def previousGray = previous(CV_BGR2GRAY, 1)

  def isReady = (buf.size > 1)

  def cycle(newBgr : IplImage)
  {
    if (isReady) {
      val oldMap = buf.remove(0)
      oldMap.values.foreach(_.release)
    }
    val newMap = new mutable.LinkedHashMap[Int, IplImage]
    newMap.put(NO_CONVERSION, newBgr)
    buf += newMap
  }

  def clear()
  {
    buf.foreach(_.values.foreach(_.release))
    buf.clear
  }
}
