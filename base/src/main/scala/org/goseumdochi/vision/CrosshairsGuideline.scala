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

class CrosshairsGuideline(val settings : Settings)
    extends VisionAugmenter
{
  private val includeCircle = settings.Vision.crosshairsCircle

  private val radius =
    (settings.BodyRecognition.minRadius +
      settings.BodyRecognition.maxRadius) / 2

  override def augmentFrame(
    overlay : RetinalOverlay, frameTime : TimePoint,
    hintBodyPos : Option[PlanarPos])
  {
    val xCenter = overlay.corner.x / 2
    val yCenter = overlay.corner.y / 2
    val center = RetinalPos(xCenter, yCenter)
    val top = RetinalPos(xCenter, 0)
    val bottom = RetinalPos(xCenter, overlay.corner.y)
    val left = RetinalPos(0, yCenter)
    val right = RetinalPos(overlay.corner.x, yCenter)
    overlay.drawLineSegment(top, bottom, NamedColor.YELLOW, 1)
    overlay.drawLineSegment(left, right, NamedColor.YELLOW, 1)

    if (includeCircle) {
      overlay.drawCircle(center, radius, NamedColor.YELLOW, 1)
    }
  }
}

