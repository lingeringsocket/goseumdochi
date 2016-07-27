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

import collection._

import BlobAnalysis._
import BodyDetector._

class FlashyBodyDetector(
  val settings : Settings,
  val retinalTransformProvider : RetinalTransformProvider)
    extends BodyDetector
{
  private val random = scala.util.Random

  class BodyMotionDetector extends MotionDetector(
    settings, retinalTransformProvider,
    new IgnoreLarge(settings.MotionDetection.bodyThreshold),
    MotionDetector.GravitySorter)

  private[vision] val motionDetector = new BodyMotionDetector

  override def analyzeFrame(
    imageDeck : ImageDeck, frameTime : TimePoint,
    hintBodyPos : Option[PlanarPos])
      : Iterable[VisionActor.AnalyzerResponseMsg] =
  {
    val randomColor = {
      if (random.nextBoolean) {
        NamedColor.WHITE
      } else {
        NamedColor.BLACK
      }
    }
    val light = Iterable(VisionActor.RequireLightMsg(randomColor, frameTime))
    val motion =
      motionDetector.detectMotion(
        imageDeck.currentGray, frameTime).map(
        motionMsg => {
          val msg = BodyDetectedMsg(motionMsg.pos, frameTime)
          newDebugger(imageDeck.currentBgr) { overlay =>
            msg.renderOverlay(overlay)
          }
          msg
        }
      )
    light ++ motion
  }
}
