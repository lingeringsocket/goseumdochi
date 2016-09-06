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

package org.goseumdochi.android.watchdog

import org.goseumdochi.android._
import org.goseumdochi.android.lib._
import org.goseumdochi.android.R
import org.goseumdochi.android.TR

import android.graphics._

import java.text._
import java.util._
import java.util.concurrent._

import android.hardware.Camera

class WatchdogControlView(
  context : WatchdogControlActivity,
  retinalInput : AndroidRetinalInput,
  outputQueue : ArrayBlockingQueue[Bitmap])
    extends ControlViewBase(context, retinalInput, outputQueue)
    with TypedFindView
{
  private var frameNumber = 0

  private val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")

  private val linearLayout = context.findView(TR.control_linear_layout)

  private val timeTextView = context.findView(TR.control_status_time)

  private val frameTextView = context.findView(TR.control_status_frame)

  private val robotTextView = context.findView(TR.control_status_robot)

  private val msgTextView = context.findView(TR.control_status_message)

  override protected def onDraw(canvas : Canvas)
  {
    super.onDraw(canvas)

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

  override protected def drawFrame(
    canvas : Canvas, bitmap : Bitmap, offsetX : Int, offsetY : Int)
  {
    super.drawFrame(canvas, bitmap, offsetY, offsetY)
    linearLayout.setPadding(offsetX, offsetY, 0, 0)
    frameNumber += 1
  }
}
