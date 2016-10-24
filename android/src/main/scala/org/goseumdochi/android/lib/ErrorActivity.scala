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

import org.goseumdochi.android.common.R

import android.content._
import android.net._
import android.os._
import android.view._

abstract class ErrorActivity(
  viewId : Int,
  nextActivity : Class[_])
    extends MainMenuActivityBase
{
  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    setContentView(viewId)
  }

  def onOkClicked(v : View)
  {
    val intent = new Intent(this, nextActivity)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    finish
    startActivity(intent)
  }

  def onHelpClicked(v : View)
  {
    val intent = new Intent(
      Intent.ACTION_SENDTO,
      Uri.parse("mailto:info@goseumdochi.org"))
    intent.putExtra(Intent.EXTRA_SUBJECT, getSubject)
    startActivity(Intent.createChooser(
      intent,
      getString(R.string.help_chooser)))
  }

  protected def getSubject : String = "Help Me!"
}
