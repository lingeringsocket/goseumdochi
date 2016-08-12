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

import android.graphics._

import java.util.concurrent._

class LeashControlView(
  context : LeashControlActivity,
  retinalInput : AndroidRetinalInput,
  outputQueue : ArrayBlockingQueue[Bitmap])
    extends ControlViewBase(context, retinalInput, outputQueue)
    with TypedFindView
{
  private val statusTextView = context.findView(TR.control_status)

  override protected def onDraw(canvas : Canvas)
  {
    super.onDraw(canvas)

    val red = new Paint
    red.setColor(Color.RED)
    red.setStrokeWidth(5)

    val blue = new Paint
    blue.setColor(Color.BLUE)
    blue.setStrokeWidth(5)

    val left = 0f
    val right = canvas.getWidth
    val top = 0f
    val bottom = canvas.getHeight
    val centerX = (left + right) / 2
    val centerY = (top + bottom) / 2

    val force = context.getForce
    val tipX = force.x
    val tipY = force.y
    val redX = centerX + tipX
    val redY = centerY + tipY
    val blueX = centerX - tipX
    val blueY = centerY - tipY

    canvas.drawLine(centerX, centerY, redX.toFloat, redY.toFloat, red)
    canvas.drawLine(centerX, centerY, blueX.toFloat, blueY.toFloat, blue)

    statusTextView.setText(context.getState)
  }

  override protected def drawFrame(
    canvas : Canvas, bitmap : Bitmap, offsetX : Int, offsetY : Int)
  {
    if (context.isOrienting) {
      super.drawFrame(canvas, bitmap, offsetX, offsetY)
    }
  }
}
