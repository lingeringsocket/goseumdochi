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

import android.graphics._
import android.hardware._
import android.view._

import java.io._
import java.text._
import java.util._
import java.util.concurrent._

import android.hardware.Camera

import org.bytedeco.javacv._

import org.goseumdochi.common._

class ControlView(
  context : ControlActivity,
  retinalInput : AndroidRetinalInput,
  outputQueue : ArrayBlockingQueue[Bitmap])
    extends View(context) with Camera.PreviewCallback with View.OnTouchListener
{
  private var frameNumber = 0

  private val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")

  private val linearLayout = context.findView(TR.control_linear_layout)

  private val timeTextView = context.findView(TR.control_status_time)

  private val frameTextView = context.findView(TR.control_status_frame)

  private val robotTextView = context.findView(TR.control_status_robot)

  private val msgTextView = context.findView(TR.control_status_message)

  private val paint = new Paint

  override def onTouch(v : View, e : MotionEvent) =
  {
    context.getVisionActor.foreach(
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
    canvas.drawARGB(255, 0, 0, 0)

    if (!outputQueue.isEmpty) {
      val bitmap = outputQueue.take
      val offsetX = (canvas.getWidth - bitmap.getWidth) / 2
      val offsetY = (canvas.getHeight - bitmap.getHeight) / 2
      linearLayout.setPadding(offsetX, offsetY, 0, 0)
      canvas.drawBitmap(bitmap, offsetX, offsetY, paint)
      frameNumber += 1
    }

    val timeStamp = sdf.format(Calendar.getInstance.getTime)
    val robotState = context.getRobotState
    val lastVoiceMessage = context.getVoiceMessage

    timeTextView.setText(
      context.getString(R.string.control_status_time_text) + " " + timeStamp)
    frameTextView.setText(
      context.getString(R.string.control_status_frame_text) + " " + frameNumber)
    robotTextView.setText(
      context.getString(R.string.control_status_robot_text) + " " + robotState)
    msgTextView.setText(
      context.getString(R.string.control_status_message_text) +
        " " + lastVoiceMessage)
  }
}
