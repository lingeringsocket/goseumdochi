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
import android.graphics.drawable._
import android.net._
import android.os._
import android.preference._
import android.text.method._
import android.view._

trait LeashMainMenuActivityBase
    extends MainMenuActivityBase with TypedFindView
{
  override protected def startAboutActivity()
  {
    val intent = new Intent(this, classOf[LeashAboutActivity])
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
    val intent = new Intent(this, classOf[LeashSettingsActivity])
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    startActivity(intent)
  }
}

class LeashMainActivity
    extends MainActivityBase
    with LeashMainMenuActivityBase
{
  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    LeashAnalytics.init(getApplication)
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    setContentView(R.layout.main)
    findView(TR.intro_post_text).setMovementMethod(
      LinkMovementMethod.getInstance)
    val img = findView(TR.intro_animation_image)
    img.setBackgroundResource(R.drawable.intro_animation)
    img.getBackground.asInstanceOf[AnimationDrawable].start
  }

  override protected def onResume()
  {
    super.onResume
    val preTextView = findView(TR.intro_pre_text)
    val postTextView = findView(TR.intro_post_text)
    if (walkthroughSeen) {
      preTextView.setText(getString(R.string.intro_expert_text))
      postTextView.setVisibility(View.VISIBLE)
    } else {
      preTextView.setText(getString(R.string.intro_pre_text))
      postTextView.setVisibility(View.INVISIBLE)
    }
    LeashAnalytics.trackScreen("Intro")
  }

  private def walkthroughSeen() =
  {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    prefs.getBoolean(
      LeashSettingsActivity.PREF_WALKTHROUGH, false)
  }

  override def onStartClicked(v : View)
  {
    if (walkthroughSeen) {
      super.onStartClicked(v)
    } else {
      startNextActivity
    }
  }

  override protected def startNextActivity()
  {
    val activityClass = {
      if (walkthroughSeen) {
        classOf[LeashControlActivity]
      } else {
        classOf[LeashWalkthroughActivity]
      }
    }
    val intent = new Intent(this, activityClass)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    startActivity(intent)
  }
}
