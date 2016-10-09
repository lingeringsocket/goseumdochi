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

import LeashControlActivity.LeashState._

class LeashControlView(
  context : LeashControlActivity,
  retinalInput : AndroidRetinalInput,
  outputQueue : ArrayBlockingQueue[Bitmap])
    extends ControlViewBase(context, retinalInput, outputQueue)
    with TypedFindView
{
  private val statusTextView = context.findView(TR.control_status)

  private var lastYank = 0L

  override protected def onDraw(canvas : Canvas)
  {
    super.onDraw(canvas)

    if (context.getState != ATTACHING) {
      val paint = new Paint
      paint.setColor(Color.WHITE)
      paint.setStrokeWidth(5)
      paint.setStyle(Paint.Style.STROKE)

      val left = 0f
      val right = canvas.getWidth
      val top = 0f
      val bottom = canvas.getHeight
      val centerX = (left + right) / 2
      val centerY = (top + bottom) / 2

      val leash = context.getLeash
      renderMotion(
        canvas, centerX, centerY, leash.sampleMagnitude, paint)
      val newYank = leash.getLastYank
      if (newYank != lastYank) {
        LeashAnalytics.trackEvent("yank", "timestamp" + newYank)
        lastYank = newYank
      }
    }

    statusTextView.setText(context.getStateText)
  }

  private def renderMotion(
    canvas : Canvas,
    centerX : Float, centerY : Float,
    motion : Double, paint : Paint)
  {
    val step = 60.0f
    var r = Math.min((motion*300).toFloat, 300.0f)
    while (r > step) {
      canvas.drawRect(
        centerX - r, centerY - r, centerX + r, centerY + r, paint)
      r -= step
    }
  }

  override protected def drawFrame(
    canvas : Canvas, bitmap : Bitmap, offsetX : Int, offsetY : Int)
  {
    if (context.isOrienting) {
      super.drawFrame(canvas, bitmap, offsetX, offsetY)
    } else {
      val color = context.getState match {
        case SITTING => Color.MAGENTA
        case WALKING => Color.GREEN
        case RUNNING => Color.CYAN
        case _ => Color.BLACK
      }
      canvas.drawColor(color)
    }
  }
}
