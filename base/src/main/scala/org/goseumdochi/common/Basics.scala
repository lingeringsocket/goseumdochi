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

package org.goseumdochi.common

import scala.math._
import scala.annotation._

import scala.concurrent.duration._
import org.bytedeco.javacpp.helper.opencv_core._

case class TimePoint(d : TimeSpan)
{
  def <(that : TimePoint) = (this.d < that.d)
  def <=(that : TimePoint) = (this.d <= that.d)
  def >(that : TimePoint) = (this.d > that.d)
  def >=(that : TimePoint) = (this.d >= that.d)
  def -(that : TimePoint) = (this.d - that.d)

  def +(that : TimeSpan) = TimePoint(this.d + that)
  def -(that : TimeSpan) = TimePoint(this.d - that)
}

case object TimePoint
{
  private val bias = System.currentTimeMillis

  def now = TimePoint(TimeSpan(System.currentTimeMillis - bias, MILLISECONDS))

  final val ZERO = TimePoint(TimeSpan(0, MILLISECONDS))

  final val ONE = TimePoint(TimeSpan(1000, MILLISECONDS))

  final val TEN = TimePoint(TimeSpan(10000, MILLISECONDS))
}

case object TimeSpans
{
  // FIXME
  final val INDEFINITE = TimeSpan(1000, SECONDS)
}

trait EventMsg
{
  def eventTime : TimePoint
}

trait PlanarVector
{
  def x : Double
  def y : Double
  def construct(x : Double, y : Double) : PlanarVector

  override def toString = "(" + x + ", " + y + ")"
}

case class PlanarPos(
  x : Double,
  y : Double
) extends PlanarVector
{
  override def construct(x : Double, y : Double) : PlanarPos = PlanarPos(x, y)
}

case class PlanarFreeVector(
  x : Double,
  y : Double
) extends PlanarVector
{
  override def construct(x : Double, y : Double)
      : PlanarFreeVector = PlanarFreeVector(x, y)
}

case class RetinalPos(
  x : Double,
  y : Double
) extends PlanarVector
{
  override def construct(x : Double, y : Double) : RetinalPos = RetinalPos(x, y)
}

case class PolarImpulse(
  speed : Double,
  duration : TimeSpan,
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
  final val PI = Pi

  final val TWO_PI = 2.0*Pi

  final val HALF_PI = Pi/2.0

  def sqr(n : Int) = (n*n)

  def sqr(n : Double) = (n*n)

  def sgn(n : Double) = (if (n < 0.0) -1.0 else 1.0)

  @tailrec def normalizeRadians(theta : Double) : Double =
  {
    if (theta <= -PI) {
      normalizeRadians(theta + TWO_PI)
    } else if (theta > PI) {
      normalizeRadians(theta - TWO_PI)
    } else {
      theta
    }
  }

  def normalizeRadiansPositive(theta : Double) : Double =
  {
    val normalized = normalizeRadians(theta)
    if (normalized < 0) {
      normalized + TWO_PI
    } else {
      normalized
    }
  }

  def midpoint[PV <: PlanarVector](p1 : PV, p2 : PV) : PV =
    p1.construct((p1.x + p2.x) / 2, (p1.y + p2.y) / 2).asInstanceOf[PV]

  def vectorSum[PV <: PlanarVector](p1 : PV, p2 : PV) : PV =
    p1.construct(p1.x + p2.x, p1.y + p2.y).asInstanceOf[PV]

  def vectorDiff[PV <: PlanarVector](p1 : PV, p2 : PV) : PV =
    p1.construct(p1.x - p2.x, p1.y - p2.y).asInstanceOf[PV]

  def vectorScaled[PV <: PlanarVector](pv : PV, s : Double) : PV =
    pv.construct(pv.x*s, pv.y*s).asInstanceOf[PV]

  def vectorDot[PV <: PlanarVector](p1 : PV, p2 : PV) : Double =
  {
    p1.x*p2.x + p1.y*p2.y
  }

  def polarMotion(pv : PlanarVector) : PolarVector =
  {
    val r = hypot(pv.x, pv.y)
    val theta = atan2(pv.y, pv.x)
    PolarVector(r, normalizeRadians(theta))
  }

  def polarMotion(p1 : PlanarVector, p2 : PlanarVector) : PolarVector =
  {
    polarMotion(vectorDiff(p2, p1))
  }

  def predictMotion(impulse : PolarImpulse) =
    PolarVector(
      impulse.speed * impulse.duration.toMillis / 1000.0,
      impulse.theta)
}

object NamedColor
{
  final val BLACK = AbstractCvScalar.BLACK
  final val BLUE = AbstractCvScalar.BLUE
  final val CYAN = AbstractCvScalar.CYAN
  final val GRAY = AbstractCvScalar.GRAY
  final val GREEN = AbstractCvScalar.GREEN
  final val MAGENTA = AbstractCvScalar.MAGENTA
  final val RED = AbstractCvScalar.RED
  final val WHITE = AbstractCvScalar.WHITE
  final val YELLOW = AbstractCvScalar.YELLOW
}
