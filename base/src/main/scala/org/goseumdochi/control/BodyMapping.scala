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

package org.goseumdochi.control

import org.goseumdochi.common._
import org.goseumdochi.common.MoreMath._

import scala.concurrent.duration._

case class BodyMapping(
  scale : Double,
  thetaOffset : Double)
{
  def transformMotion(motion : PolarVector, speed : Double) =
  {
    val duration = (motion.distance / speed) / scale
    val theta = normalizeRadians(motion.theta - thetaOffset)
    PolarImpulse(
      speed,
      TimeSpan((duration*1000.0).toLong, MILLISECONDS),
      theta)
  }

  def computeImpulse(
    origin : PlanarPos, dest : PlanarPos,
    speed : Double, extraTime : TimeSpan) =
  {
    val motion = polarMotion(origin, dest)
    val impulse = transformMotion(motion, speed)
    PolarImpulse(impulse.speed, impulse.duration + extraTime, impulse.theta)
  }
}
