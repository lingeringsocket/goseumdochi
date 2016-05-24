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

package org.goseumdochi.behavior

import org.goseumdochi.common._
import org.goseumdochi.common.MoreMath._

import scala.concurrent.duration._

object PacingFsm
{
  private val forwardImpulse = PolarImpulse(
    0.5, 500.milliseconds, 0.0)

  private val backwardImpulse = PolarImpulse(
    0.5, 500.milliseconds, PI)
}
import PacingFsm._

class PacingFsm() extends CyclicFsm(Array(
  forwardImpulse, forwardImpulse, backwardImpulse, backwardImpulse))
{
}
