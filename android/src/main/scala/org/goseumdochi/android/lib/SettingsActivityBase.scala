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

import android.content._
import android.preference._

object SettingsActivity
{
  final val PREF_WHITE_BALANCE = "pref_key_white_balance"
}

abstract class SettingsActivityBase
    extends PreferenceActivity with ActivityBaseNoCompat
    with SharedPreferences.OnSharedPreferenceChangeListener
{
  private def getPrefs = PreferenceManager.getDefaultSharedPreferences(this)

  override protected def onPause()
  {
    super.onPause
    getPrefs.unregisterOnSharedPreferenceChangeListener(this)
  }

  override protected def onResume()
  {
    super.onResume
    getPrefs.registerOnSharedPreferenceChangeListener(this)
  }

  override def onSharedPreferenceChanged(
    prefs : SharedPreferences, key : String)
  {
  }
}
