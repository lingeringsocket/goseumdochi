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

class TestActuator extends Actuator
{
  private var lastImpulse : Option[PolarImpulse] = None

  private var lastColor : Option[java.awt.Color] = None

  private var lastTwirl : Option[TimeSpan] = None

  override def actuateMotion(impulse : PolarImpulse)
  {
    lastImpulse = Some(impulse)
  }

  override def actuateLight(color : java.awt.Color)
  {
    lastColor = Some(color)
  }

  override def actuateTwirl(degrees : Int, duration : TimeSpan)
  {
    lastTwirl = Some(duration)
  }

  def reset()
  {
    lastImpulse = None
    lastColor = None
    lastTwirl = None
  }

  def retrieveImpulse() : Option[PolarImpulse] = lastImpulse

  def retrieveColor() : Option[java.awt.Color] = lastColor

  def retrieveTwirl() : Option[TimeSpan] = lastTwirl
}
