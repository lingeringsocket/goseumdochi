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
import android.text.method._
import android.view._
import android.widget._

abstract class ErrorActivity(
  viewId : Int, linkView : Option[TypedResource[TextView]] = None)
    extends MainMenuActivityBase
{
  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    setContentView(viewId)
    linkView.foreach(
      findView(_).setMovementMethod(LinkMovementMethod.getInstance))
  }

  def onOkClicked(v : View)
  {
    val intent = new Intent(this, classOf[SetupActivity])
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    finish
    startActivity(intent)
  }
}

class BumpActivity extends ErrorActivity(R.layout.bump)

class LostActivity extends ErrorActivity(R.layout.lost)

class UnfoundActivity extends ErrorActivity(R.layout.unfound)

class BluetoothErrorActivity extends ErrorActivity(
  R.layout.bluetooth, Some(TR.bluetooth_error_content))
