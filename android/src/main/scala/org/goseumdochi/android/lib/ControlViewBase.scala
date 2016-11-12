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

package org.goseumdochi.android.lib

import org.goseumdochi.common._

import android.graphics._
import android.hardware._
import android.view._

import android.hardware.Camera

import org.bytedeco.javacv._

import java.io._
import java.util.concurrent._

abstract class ControlViewBase(
  context : ControlActivityBase,
  retinalInput : AndroidRetinalInput,
  outputQueue : ArrayBlockingQueue[Bitmap])
    extends View(context) with Camera.PreviewCallback with View.OnTouchListener
{
  private val paint = new Paint

  override def onTouch(v : View, e : MotionEvent) =
  {
    context.getTheaterListener.foreach(
      _.onTheaterClick(RetinalPos(e.getX, e.getY)))
    true
  }

  override def onPreviewFrame(data : Array[Byte], camera : Camera)
  {
    try {
      if (context.isRobotConnected) {
        if (retinalInput.needsFrame) {
          val bitmap = convertToBitmap(data, camera)
          val converter = new AndroidFrameConverter
          val frame = converter.convert(bitmap)
          retinalInput.pushFrame(frame)
        }
      } else {
        if (outputQueue.isEmpty) {
          val bitmap = convertToBitmap(data, camera)
          outputQueue.put(bitmap)
          postInvalidate
        }
      }
      camera.addCallbackBuffer(data)
    } catch {
      // need to swallow these to prevent spurious crashes
      case e : RuntimeException =>
    }
  }

  private def convertToBitmap(data : Array[Byte], camera : Camera) =
  {
    val size = camera.getParameters.getPreviewSize
    val out = new ByteArrayOutputStream
    val yuv = new YuvImage(
      data, ImageFormat.NV21, size.width, size.height, null)
    yuv.compressToJpeg(
      new android.graphics.Rect(0, 0, size.width, size.height), 50, out)
    val bytes = out.toByteArray
    BitmapFactory.decodeByteArray(bytes, 0, bytes.length)
  }

  override protected def onDraw(canvas : Canvas)
  {
    // janky way to hide the underlying camera preview...
    // apparently these days we should be using SurfaceTexture instead
    // (and camera2 API for that matter)
    canvas.drawColor(getDefaultColor)

    if (!outputQueue.isEmpty) {
      val bitmap = outputQueue.take
      val offsetX = (canvas.getWidth - bitmap.getWidth) / 2
      val offsetY = (canvas.getHeight - bitmap.getHeight) / 2
      drawFrame(canvas, bitmap, offsetX, offsetY)
    }
  }

  protected def drawFrame(
    canvas : Canvas, bitmap : Bitmap, offsetX : Int, offsetY : Int)
  {
    canvas.drawBitmap(bitmap, offsetX, offsetY, paint)
  }

  protected def getDefaultColor = Color.BLACK
}
