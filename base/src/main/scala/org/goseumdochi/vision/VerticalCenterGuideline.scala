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

object VerticalCenterGuideline
{
  // result messages
  final case class VerticalCenterOverlayMsg(eventTime : TimePoint)
      extends VisionActor.AnalyzerResponseMsg
  {
    override def renderOverlay(overlay : RetinalOverlay)
    {
      val center = overlay.corner.x / 2
      val top = RetinalPos(center, 0)
      val bottom = RetinalPos(center, overlay.corner.y)
      overlay.drawLineSegment(top, bottom, NamedColor.YELLOW, 1)
    }
  }
}
import VerticalCenterGuideline._

class VerticalCenterGuideline(
  val settings : Settings, val xform : RetinalTransform)
    extends VisionAnalyzer
{
  override def analyzeFrame(
    img : IplImage, prevImg : IplImage, gray : IplImage, prevGray : IplImage,
    frameTime : TimePoint, hintBodyPos : Option[PlanarPos])
      : Iterable[VerticalCenterOverlayMsg] =
  {
    Some(VerticalCenterOverlayMsg(frameTime))
  }
}

