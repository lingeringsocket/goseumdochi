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

class RetinalTransformGuideline(val settings : Settings)
    extends VisionAugmenter
{
  val expiration = settings.Vision.transformGuidelineExpiration

  var endTimeOpt : Option[TimePoint] = None

  override def augmentFrame(
    overlay : RetinalOverlay, frameTime : TimePoint,
    hintBodyPos : Option[PlanarPos])
  {
    endTimeOpt match {
      case Some(endTime) => {
        if (frameTime > endTime) {
          return
        }
      }
      case _ => {
        if (expiration.length > 0) {
          endTimeOpt = Some(frameTime + expiration)
        }
      }
    }
    overlay.xform match {
      case rpt : RestrictedPerspectiveTransform => {
        rpt.visualize(overlay)
      }
      case _ =>
    }
  }
}
