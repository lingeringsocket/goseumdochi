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

object BodyDetector
{
  // result messages
  final case class BodyDetectedMsg(pos : PlanarPos, eventTime : TimePoint)
      extends VisionActor.ObjDetectedMsg
  {
    override def renderOverlay(overlay : RetinalOverlay)
    {
      overlay.drawCircle(
        overlay.xform.worldToRetina(pos), 30, NamedColor.RED, 2)
    }
  }
}

trait BodyDetector extends VisionAnalyzer
{
  protected val conf = settings.BodyRecognition.subConf
}
