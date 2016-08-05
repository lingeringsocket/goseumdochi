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

import org.goseumdochi.android.lib._
import org.goseumdochi.common.MoreMath._

import android.hardware._
import android.os._

class LeashControlActivity extends ControlActivityBase
    with TypedFindView
{
  private var rotationVector : Option[Sensor] = None

  private var rotationLast : Double = Double.MaxValue

  private var rotationBaseline : Double = Double.MaxValue

  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    initSensorMgr()
    rotationVector =
      Option(sensorMgr.get.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR))
  }

  override protected def onStart()
  {
    super.onStart
    sensorMgr.foreach(sm => {
      rotationVector.foreach(sensor => {
        sm.registerListener(
          this, sensor, 10000)
      })
    })
  }

  override protected def createControlView() =
  {
    new LeashControlView(this, retinalInput, outputQueue)
  }

  override protected def startCamera()
  {
    setContentView(R.layout.control)
    val layout = findView(TR.control_preview)
    // this will cause instantiation of (lazy) preview and controlView
    layout.addView(preview)
    layout.addView(controlView)
    controlView.setOnTouchListener(controlView)
    findView(TR.control_linear_layout).bringToFront
  }

  override protected def pencilsDown()
  {
    super.pencilsDown
    rotationVector = None
    rotationBaseline = Double.MaxValue
  }

  override def onSensorChanged(event : SensorEvent)
  {
    if (rotationVector.isEmpty) {
      return
    }
    event.sensor.getType match {
      case Sensor.TYPE_GAME_ROTATION_VECTOR => {
        rotationLast = 2.0*Math.asin(event.values(2))
        if (rotationBaseline == Double.MaxValue) {
          rotationBaseline = rotationLast
        }
      }
      case _ =>
    }
  }

  override def getRotationCompensation =
  {
    normalizeRadians(rotationLast - rotationBaseline)
  }
}
