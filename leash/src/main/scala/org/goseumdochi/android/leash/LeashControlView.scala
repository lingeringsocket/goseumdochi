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
  private val linearLayout = context.findView(TR.control_linear_layout)

  private val statusTextView = context.findView(TR.control_status)

  override protected def onDraw(canvas : Canvas)
  {
    super.onDraw(canvas)

    statusTextView.setText(
      context.getString(R.string.control_status_text) + " " +
        context.getRobotState + " / " + context.getRotationCompensation)
  }
}
