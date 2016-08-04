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

import android._
import android.content._
import android.net._
import android.os._
import android.preference._
import android.text.method._

trait WatchdogMainMenuActivityBase
    extends MainMenuActivityBase with TypedFindView
{
  override protected def startAboutActivity()
  {
    val intent = new Intent(this, classOf[WatchdogAboutActivity])
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    startActivity(intent)
  }

  override protected def startHelpActivity()
  {
    val uri = Uri.parse(getString(R.string.help_url))
    startActivity(new Intent(Intent.ACTION_VIEW, uri))
  }

  override protected def startBugsActivity()
  {
    val uri = Uri.parse(getString(R.string.bugs_url))
    startActivity(new Intent(Intent.ACTION_VIEW, uri))
  }

  override protected def startSettingsActivity()
  {
    val intent = new Intent(this, classOf[WatchdogSettingsActivity])
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    startActivity(intent)
  }
}

class WatchdogMainActivity extends MainActivityBase
    with WatchdogMainMenuActivityBase
{
  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    setContentView(R.layout.main)
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val enableVoice = prefs.getBoolean(
      WatchdogSettingsActivity.PREF_ENABLE_VOICE, true)
    GlobalTts.init(getApplicationContext, enableVoice)
    GlobalVideo.init(getApplicationContext, this)
    findView(TR.cctv_text).setMovementMethod(LinkMovementMethod.getInstance)
    findView(TR.step_3_text).setMovementMethod(LinkMovementMethod.getInstance)
    requestPrerequisites
  }

  override protected def onDestroy()
  {
    GlobalTts.shutdown
    GlobalVideo.shutdown
    super.onDestroy
  }

  override protected def startNextActivity()
  {
    val intent = new Intent(this, classOf[SetupActivity])
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    startActivity(intent)
  }
}
