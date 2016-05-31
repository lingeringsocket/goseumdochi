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

import android.content._
import android.graphics._
import android.hardware._
import android.view._

import android.hardware.Camera

class SetupView(context : Context)
    extends View(context) with Camera.PreviewCallback
{
  override def onPreviewFrame(data : Array[Byte], camera : Camera)
  {
    try {
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
    val height = 50
    red.setTextSize(height)

    val left = 0f
    val right = canvas.getWidth
    val top = 0f
    val bottom = canvas.getHeight
    val centerX = (left + right) / 2
    val centerY = (top + bottom) / 2

    canvas.drawLine(left, centerY, right, centerY, yellow)
    canvas.drawLine(centerX, top, centerX, bottom, yellow)
  }
}
