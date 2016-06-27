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
import android.os._
import android.view._

class SetupActivity extends ActivityBaseNoCompat
{
  lazy val setupView = new SetupView(this)
  lazy val preview = new CameraPreview(this, setupView)

  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)

    getWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    getWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    startCamera
  }

  private def startCamera()
  {
    setContentView(R.layout.setup)
    val layout = findView(TR.setup_preview)
    layout.addView(preview)
    layout.addView(setupView)
    findView(TR.setup_instructions).bringToFront
    findView(TR.button_start).bringToFront
  }

  def onConnectClicked(v : View)
  {
    preview.closeCamera
    val intent = new Intent(this, classOf[ControlActivity])
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    startActivity(intent)
  }
}
