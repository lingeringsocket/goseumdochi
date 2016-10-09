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

package org.goseumdochi.android.leash

import LeashControlActivity._
import LeashState._

import org.goseumdochi.common._
import org.goseumdochi.common.MoreMath._

import scala.concurrent.duration._

import android.hardware._

object VirtualLeash
{
  val TENTH_SEC = 10000000L

  val HALF_SEC = TENTH_SEC*5

  val ONE_SEC = HALF_SEC*2

  val SEVEN_TENTHS_SEC = TENTH_SEC*7

  val THREE_SEC = ONE_SEC*3

  val RESTING_MAX = 0.2
}

import VirtualLeash._

class VirtualLeash(restThreshold : Long)
{
  private var velocity = PlanarFreeVector(0, 0)

  private var peakForce = PlanarFreeVector(0, 0)

  private var peakLock = false

  private var jerk = false

  private var jerkLast = false

  private var twirl = false

  private var iLatest = 0L

  private var iLastRest = 0L

  private var iLastMotion = 0L

  private var iLastMotionStart = 0L

  private var force = PlanarFreeVector(0, 0)

  private var peakMotion = PolarVector(0, 0)

  private var lastImpulse = PolarImpulse(0, 0.seconds, 0)

  private var lastYank = 0L

  private var magnitudeSum = 0.0

  private var magnitudeCount = 0

  def processEvent(event : SensorEvent) =
  {
    iLatest = event.timestamp
    val acceleration = PlanarFreeVector(
      event.values(0).toDouble,
      event.values(1).toDouble)
    val polar = polarMotion(acceleration)
    val magnitude = polar.distance
    magnitudeSum += magnitude
    magnitudeCount += 1
    var jerkNow = false
    if (magnitude > RESTING_MAX) {
      iLastMotion = iLatest
      if (iLastMotionStart == 0) {
        iLastMotionStart = iLastMotion
      }
      if ((magnitude > 7.0) && !peakLock) {
        jerkNow = true
      }
      if (magnitude > 10.0) {
        jerkNow = true
      }
      if (!peakLock && jerkNow) {
        jerk = true
      }
    } else {
      if ((iLatest - iLastMotion) > restThreshold) {
        iLastRest = iLatest
      }
    }
    (jerkNow, acceleration)
  }

  def updateVelocity(
    lastTime : Long,
    acceleration : PlanarFreeVector)
  {
    val dT = (iLatest - lastTime) / 1000000.0
    velocity = vectorSum(
      vectorScaled(acceleration, dT), velocity)
  }

  def clear()
  {
    velocity = PlanarFreeVector(0, 0)
    peakForce = velocity
    iLastMotionStart = 0
    peakLock = false
    jerk = false
    jerkLast = false
    twirl = false
  }

  def isRobotStopped = (lastImpulse.speed == 0)

  def isResting = (iLatest == iLastRest)

  def getLastImpulse = lastImpulse

  def getForce = force

  def getPeakMotion = peakMotion

  def calculateState(turnAngle : Double) =
  {
    val robotForce = PlanarFreeVector(-velocity.y , velocity.x)
    val motion = polarMotion(robotForce)
    peakMotion = polarMotion(peakForce)
    if (!peakLock && (motion.distance > peakMotion.distance)) {
      peakForce = robotForce
      peakMotion = polarMotion(peakForce)
    }
    force = PlanarFreeVector(peakForce.x , -peakForce.y)
    val dotProduct = vectorDot(robotForce, peakForce)
    val strength = peakMotion.distance
    val big = (strength > 300.0)
    if (big &&
      ((dotProduct < 0) || (motion.distance < 0.6*strength)))
    {
      peakLock = true
    }
    val sustained = (iLastMotion - iLastMotionStart) > 3*TENTH_SEC
    val sufficientPause = (iLatest - lastYank) > HALF_SEC
    var newState = SITTING
    if (big && sustained && sufficientPause
      && (lastImpulse.speed == 0) && !twirl)
    {
      if (!jerk && (Math.abs(turnAngle) > 0.25*HALF_PI)) {
        twirl = true
        newState = TWIRLING
      } else {
        if (jerk) {
          jerkLast = true
          newState = RUNNING
        } else {
          newState = WALKING
        }
      }
    }
    newState
  }

  def stopRequested(jerkNow : Boolean, turnAngle : Double)
      : Boolean =
  {
    if (lastImpulse.speed > 0) {
      val jerkStop = (!jerkLast && jerkNow)
      val jerkExpired = (jerkLast && ((iLatest - lastYank) > (10*ONE_SEC)))
      val jerkFresh = (jerkLast && ((iLatest - lastYank) < (5*ONE_SEC)))
      val restStop = ((iLatest == iLastRest) && !jerkFresh)
      val turnStop = (Math.abs(turnAngle) > 0.5*HALF_PI)
      if (restStop || jerkExpired || jerkStop || turnStop) {
        return true
      }
    }
    return false
  }

  def getLastYank = lastYank

  def rememberYank(impulse : PolarImpulse)
  {
    lastYank = iLatest
    lastImpulse = impulse
  }

  def sampleMagnitude : Double =
  {
    if (magnitudeCount > 0) {
      val avg = magnitudeSum / magnitudeCount
      magnitudeSum = 0.0
      magnitudeCount = 0
      avg
    } else {
      0.0
    }
  }
}
