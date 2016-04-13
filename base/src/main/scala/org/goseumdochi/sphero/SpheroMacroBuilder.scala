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

package org.goseumdochi.sphero

import org.goseumdochi.common._
import MoreMath._

import scala.collection.mutable._

// see http://sdk.sphero.com/api-reference/api-quick-reference/#macro-commands
class SpheroMacroBuilder
{
  private val buf = new ArrayBuffer[Byte]

  private var ended = false

  private def appendByte(b : Byte)
  {
    buf += b
  }

  private def clamp[T](value : T, minValue : T, maxValue : T)
    (implicit num : Numeric[T]) : T =
  {
    import num._

    if (value > maxValue) {
      maxValue
    } else if (value < minValue) {
      minValue
    } else {
      value
    }
  }

  def getMacroBytes() =
  {
    assert(ended)
    buf.toArray
  }

  def twirl(theta : Double, duration : TimeSpan)
  {
    val degrees = (360.0*theta / TWO_PI).toInt
    val millis = duration.toMillis.toInt
    appendByte(0x1A)
    appendByte((degrees >> 8).toByte)
    appendByte(degrees.toByte)
    appendByte((millis >> 8).toByte)
    appendByte(millis.toByte)
  }

  def roll(impulse : PolarImpulse)
  {
    val degrees = (360.0*normalizeRadiansPositive(impulse.theta) / TWO_PI).toInt
    val heading = (degrees % 360).toInt
    val velocity = clamp(impulse.speed.toFloat, 0.0D, 1.0D)
    val millis = impulse.duration.toMillis.toInt
    appendByte(0x1D)
    appendByte((velocity * 255.0D).toInt.toByte)
    appendByte((heading >> 8).toByte)
    appendByte(heading.toByte)
    appendByte((millis >> 8).toByte)
    appendByte(millis.toByte)
  }

  def stop()
  {
    appendByte(0x25)
    appendByte(0x0)
    appendByte(0x0)
  }

  def delay(duration : TimeSpan)
  {
    val millis = duration.toMillis.toInt
    appendByte(0xB)
    appendByte((millis >> 8).toByte)
    appendByte(millis.toByte)
  }

  def setBackLed(on : Boolean)
  {
    appendByte(0x9)
    if (on) {
      appendByte(0xFF.toByte)
    } else {
      appendByte(0x0)
    }
    appendByte(0x0)
  }

  def end()
  {
    ended = true
    appendByte(0x1)
  }
}
