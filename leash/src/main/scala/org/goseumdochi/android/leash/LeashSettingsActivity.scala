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

import android.content._
import android.os._
import android.preference._

object LeashSettingsActivity
{
  final val PREF_WALK_SPEED = "pref_key_walk_speed"

  final val PREF_RUN_SPEED = "pref_key_run_speed"

  final val PREF_WALKTHROUGH = "pref_key_walkthrough"

  final val PREF_ANALYTICS_OPT_OUT = "pref_key_analytics_opt_out"

  private def getSpeedDefault(context : Context, key : String) =
  {
    val defaultRes = key match {
      case PREF_WALK_SPEED => R.string.pref_default_walk_speed
      case PREF_RUN_SPEED => R.string.pref_default_run_speed
    }
    context.getString(defaultRes).toInt
  }

  private def getSpeed(context : Context, key : String) =
  {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val speed = prefs.getInt(key, getSpeedDefault(context, key))
    if (speed < 10) {
      0.1
    } else {
      speed / 100.0
    }
  }

  def getWalkingSpeed(context : Context) : Double =
    getSpeed(context, PREF_WALK_SPEED)

  def getRunningSpeed(context : Context) : Double =
    getSpeed(context, PREF_RUN_SPEED)
}

import LeashSettingsActivity._

class LeashSettingsActivity extends SettingsActivityBase with TypedFindView
{
  override def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    addPreferencesFromResource(R.xml.preferences)
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    updateSpeed(prefs, PREF_WALK_SPEED)
    updateSpeed(prefs, PREF_RUN_SPEED)
  }

  override def onSharedPreferenceChanged(
    prefs : SharedPreferences, key : String)
  {
    key match {
      case PREF_WALK_SPEED | PREF_RUN_SPEED => {
        updateSpeed(prefs, key)
      }
      case PREF_ANALYTICS_OPT_OUT => {
        LeashAnalytics.setOptOut(prefs.getBoolean(key, false))
      }
      case _ =>
    }
  }

  private def updateSpeed(prefs : SharedPreferences, key : String)
  {
    val speedBar = findPreference(key)
    val titleRes = key match {
      case PREF_WALK_SPEED => R.string.pref_title_walk_speed
      case PREF_RUN_SPEED => R.string.pref_title_run_speed
    }
    val speed = prefs.getInt(key, getSpeedDefault(this, key))
    speedBar.setTitle(
      getString(titleRes) + ":  " + speed)
  }
}
