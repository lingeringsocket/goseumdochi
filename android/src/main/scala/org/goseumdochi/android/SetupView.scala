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

package org.goseumdochi.android

import org.goseumdochi.common.MoreMath._

import android.graphics._
import android.hardware._
import android.view._

import android.hardware.Camera

class SetupView(context : SetupActivity)
    extends View(context) with Camera.PreviewCallback
{
  override def onPreviewFrame(data : Array[Byte], camera : Camera)
  {
    try {
      postInvalidate
      camera.addCallbackBuffer(data)
    } catch {
      // need to swallow these to prevent spurious crashes
      case e : RuntimeException =>
    }
  }

  override protected def onDraw(canvas : Canvas)
  {
    val yellow = new Paint
    yellow.setColor(Color.YELLOW)

    val red = new Paint
    red.setColor(Color.RED)
    red.setStrokeWidth(5)

    val blue = new Paint
    blue.setColor(Color.BLUE)
    blue.setStrokeWidth(3)

    val left = 0f
    val right = canvas.getWidth
    val top = 0f
    val bottom = canvas.getHeight
    val centerX = (left + right) / 2
    val centerY = (top + bottom) / 2

    val orientation = context.getOrientation
    val theta = (TWO_PI * orientation) / 360.0
    val radius = 100f

    canvas.drawLine(left, centerY, right, centerY, yellow)
    canvas.drawLine(centerX, top, centerX, bottom, yellow)

    var stringRes = R.string.setup_instructions_camera
    if (context.canDetectOrientation) {
      val arrowX = centerX + radius*Math.cos(theta).toFloat
      val arrowY = centerY + radius*Math.sin(theta).toFloat
      val goalY = centerY - radius
      canvas.drawLine(centerX, centerY, arrowX, arrowY, red)
      canvas.drawLine(centerX, centerY, centerX, goalY, blue)
      val button = context.getConnectButton
      if (Math.abs(orientation - 270) > 5) {
        stringRes = R.string.setup_instructions_orientation
        button.setVisibility(View.GONE)
      } else {
        button.setVisibility(View.VISIBLE)
      }
    }

    context.getInstructions.setText(context.getString(stringRes))
  }
}
