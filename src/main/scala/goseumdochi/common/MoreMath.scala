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

package goseumdochi.common

import scala.math._
import scala.annotation._

case class PlanarPos(
  x : Double,
  y : Double
)

case class PolarImpulse(
  speed : Double,
  duration : Double,
  theta : Double
)

case class PolarVector(
  distance : Double,
  theta : Double
)
{
  def dx = distance*cos(theta)
  def dy = distance*sin(theta)
}

case class LinearModel(
  m : Double,
  b : Double
)

case class LinearMotionModel(
  distanceModel : LinearModel,
  thetaModel : LinearModel
)

object MoreMath
{
  final val TWO_PI = 2*Pi

  def sqr(n : Int) = (n*n)

  def sqr(n : Double) = (n*n)

  def sgn(n : Double) = (if (n < 0.0) -1.0 else 1.0)

  @tailrec def normalizeRadians(theta : Double) : Double =
  {
    if (theta < 0) {
      normalizeRadians(theta + TWO_PI)
    } else if (theta >= TWO_PI) {
      normalizeRadians(theta - TWO_PI)
    } else {
      theta
    }
  }

  def midpoint(p1 : PlanarPos, p2 : PlanarPos) =
  {
    PlanarPos((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)
  }

  def polarMotion(p1 : PlanarPos, p2 : PlanarPos) =
  {
    val x = p2.x - p1.x
    val y = p2.y - p1.y
    val r = hypot(x, y)
    val theta = atan2(y, x)
    PolarVector(r, normalizeRadians(theta))
  }

  def predictMotion(impulse : PolarImpulse) =
    PolarVector(impulse.speed * impulse.duration, impulse.theta)
}
