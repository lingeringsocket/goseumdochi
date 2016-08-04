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

import android.content._
import android.hardware._
import android.os._
import android.view._

class SetupActivity extends ActivityBaseNoCompat with TypedFindView
{
  private var orientation = 0

  private lazy val setupView = new SetupView(this)

  private lazy val preview = new CameraPreview(this, setupView)

  private lazy val orientationListener = new OrientationEventListener(
    this, SensorManager.SENSOR_DELAY_NORMAL)
  {
    override def onOrientationChanged(newOrientation : Int)
    {
      orientation = newOrientation
    }
  }

  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)

    if (orientationListener.canDetectOrientation) {
      orientationListener.enable
    } else {
      orientationListener.disable
    }

    getWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    getWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    startCamera
  }

  override protected def onDestroy
  {
    orientationListener.disable
    super.onDestroy
  }

  private def startCamera()
  {
    setContentView(R.layout.setup)
    val layout = findView(TR.setup_preview)
    layout.addView(preview)
    layout.addView(setupView)
    getInstructions.bringToFront
    getConnectButton.bringToFront
  }

  def onConnectClicked(v : View)
  {
    preview.closeCamera
    val intent = new Intent(this, classOf[ControlActivity])
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    startActivity(intent)
  }

  def canDetectOrientation = orientationListener.canDetectOrientation

  def getOrientation = orientation

  def getInstructions = findView(TR.setup_instructions)

  def getConnectButton = findView(TR.button_connect)
}
