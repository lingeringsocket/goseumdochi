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

trait RetinalTransformProvider
{
  def getRetinalTransform : RetinalTransform
}

trait RetinalTransform extends RetinalTransformProvider
{
  def retinaToWorld(pos : RetinalPos) : PlanarPos
  def worldToRetina(pos : PlanarPos) : RetinalPos
  def isValid(pos : RetinalPos) : Boolean = true
  override def getRetinalTransform = this
  def isMirrorWorld : Boolean = false
}

// assume your basic parallel projection, but make the retinal y axis point
// up instead of down, the way God intended (well, maybe not, since
// real retinal images are "upside down", but anyway...)
case object FlipRetinalTransform extends RetinalTransform
{
  override def retinaToWorld(pos : RetinalPos) : PlanarPos =
    PlanarPos(pos.x, -pos.y)

  override def worldToRetina(pos : PlanarPos) : RetinalPos =
    RetinalPos(pos.x, -pos.y)

  override def isMirrorWorld : Boolean = true
}
