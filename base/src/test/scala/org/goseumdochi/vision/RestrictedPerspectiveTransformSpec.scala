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
import org.goseumdochi.common.MoreMath._

import org.bytedeco.javacpp.opencv_imgcodecs._

class RestrictedPerspectiveTransformSpec extends VisualizableSpecification
{
  private val nearCenter = RetinalPos(555, 739)

  private val farCenter = RetinalPos(560, 436)

  private val distantCenter = RetinalPos(565, 274)

  private val nearRight = RetinalPos(1005, 739)

  private val farRight = RetinalPos(840, 436)

  private val distantRight = RetinalPos(759, 274)

  private val distantRightExpected = RetinalPos(751.7, 274.0)

  private val worldMeter = 100.0

  private val perspective = RestrictedPerspectiveTransform(
    nearCenter, farCenter, distantCenter, nearRight, farRight, worldMeter)

  def worldDistance(p1 : RetinalPos, p2 : RetinalPos) =
  {
    val w1 = perspective.retinaToWorld(p1)
    val w2 = perspective.retinaToWorld(p2)
    val motion = polarMotion(w1, w2)
    motion.distance
  }

  "RestrictedPerspectiveTransform" should
  {
    "find vanishing point" in
    {
      perspective.vanishingPoint must beRoughly(RetinalPos(568.2, -63.0))
    }

    "transform retina to world" in
    {
      worldDistance(nearCenter, farCenter) must be closeTo(worldMeter, 1.0)
      worldDistance(nearRight, farRight) must be closeTo(worldMeter, 1.0)
      worldDistance(nearCenter, nearRight) must be closeTo(worldMeter, 1.0)
      worldDistance(farCenter, farRight) must be closeTo(worldMeter, 1.0)
    }

    "transform world to retina" in
    {
      val nearRightWorld = perspective.retinaToWorld(nearRight)
      val farRightWorld = perspective.retinaToWorld(farRight)
      val motion = polarMotion(nearRightWorld, farRightWorld)
      val distantRightWorld = PlanarPos(
        farRightWorld.x + motion.dx,
        farRightWorld.y + motion.dy)
      val distantRightComputed = perspective.worldToRetina(distantRightWorld)
      if (shouldVisualize) {
        val img = cvLoadImage("data/perspective.jpg")
        visualize(img, distantRightComputed)
      }
      distantRightComputed must beRoughly(distantRightExpected)
    }
  }
}
