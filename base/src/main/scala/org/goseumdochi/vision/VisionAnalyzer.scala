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

import collection._

trait VisionAnalyzer extends AutoCloseable
{
  private val debugImages = new mutable.ArrayBuffer[IplImage]

  type OverylayRenderFunc = (RetinalOverlay) => Unit

  type Debugger = (OverylayRenderFunc) => Unit

  protected def newDebugger(inputImg : IplImage) : Debugger =
  {
    if (settings.Test.visualize) {
      val newImage = inputImg.clone
      debugImages += newImage
      val overlay = new OpenCvRetinalOverlay(
        newImage, xform, RetinalPos(newImage.width, newImage.height))
      def newDebugger(overlayRenderFunc : OverylayRenderFunc) {
        overlayRenderFunc(overlay)
      }
      newDebugger
    } else {
      def nullDebugger(func : OverylayRenderFunc) {
      }
      nullDebugger
    }
  }

  def getDebugImages = debugImages.toIndexedSeq

  def analyzeFrame(
    imageDeck : ImageDeck, frameTime : TimePoint,
    hintBodyPos : Option[PlanarPos])
      : Iterable[VisionActor.AnalyzerResponseMsg]

  def settings : Settings

  def xform : RetinalTransform

  override def close() {}

  def isLongLived() : Boolean = false
}

class NullVisionAnalyzer(val settings : Settings, val xform : RetinalTransform)
    extends VisionAnalyzer
{
  override def analyzeFrame(
    imageDeck : ImageDeck, frameTime : TimePoint,
    hintBodyPos : Option[PlanarPos])
      : Iterable[VisionActor.AnalyzerResponseMsg] =
  {
    None
  }
}
