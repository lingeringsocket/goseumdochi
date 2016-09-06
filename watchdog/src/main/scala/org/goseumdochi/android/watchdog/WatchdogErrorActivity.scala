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
import org.goseumdochi.android.R
import org.goseumdochi.android.lib._

import android.os._
import android.text.method._
import android.widget._

abstract class WatchdogErrorActivity(
  viewId : Int, linkView : Option[TypedResource[TextView]] = None)
    extends ErrorActivity(viewId, classOf[SetupActivity])
    with WatchdogMainMenuActivityBase
{
  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    linkView.foreach(
      findView(_).setMovementMethod(LinkMovementMethod.getInstance))
  }
}

class WatchdogBumpActivity extends WatchdogErrorActivity(R.layout.bump)

class WatchdogLostActivity extends WatchdogErrorActivity(R.layout.lost)

class WatchdogUnfoundActivity extends WatchdogErrorActivity(R.layout.unfound)

class WatchdogBluetoothErrorActivity extends WatchdogErrorActivity(
  R.layout.bluetooth, Some(TR.bluetooth_error_content))
