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

import android.os._
import android.text.method._
import android.widget._

abstract class LeashErrorActivity(
  viewId : Int, screenName : String,
  linkView : Option[TypedResource[TextView]] = None)
    extends ErrorActivity(viewId, classOf[LeashMainActivity])
    with LeashMainMenuActivityBase
{
  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    linkView.foreach(
      findView(_).setMovementMethod(LinkMovementMethod.getInstance))
  }

  override protected def onResume()
  {
    super.onResume
    LeashAnalytics.trackScreen(screenName)
  }
}

class LeashUnfoundActivity extends LeashErrorActivity(
  R.layout.unfound, "Unfound Error", Some(TR.unfound_content))

class LeashNoSensorActivity extends LeashErrorActivity(
  R.layout.nosensor, "Sensor Error")

class LeashBluetoothErrorActivity extends LeashErrorActivity(
  R.layout.bluetooth, "Bluetooth Error", Some(TR.bluetooth_error_content))
