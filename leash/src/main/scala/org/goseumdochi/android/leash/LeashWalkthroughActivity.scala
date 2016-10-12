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
import android.view._

class LeashWalkthroughActivity
    extends PrerequisitesActivityBase
    with TypedFindView
    with View.OnTouchListener
{
  private lazy val frameImg = loadAnimation

  private val frames = Array(
    R.drawable.walkthrough0,
    R.drawable.walkthrough1,
    R.drawable.walkthrough2,
    R.drawable.walkthrough3,
    R.drawable.walkthrough4,
    R.drawable.walkthrough5,
    R.drawable.walkthrough6
  )

  private var x1 = 0f

  private lazy val useMenu = getIntent.getBooleanExtra("menu", false)

  private var iFrame = 0

  private lazy val textArray =
    getResources.getStringArray(R.array.walkthrough_text)

  private lazy val textView = findView(TR.walkthrough_text)

  private lazy val hintView = findView(TR.walkthrough_hint)

  private lazy val buttonView = findView(TR.close_walkthrough_button)

  override def onCreateOptionsMenu(menu : Menu) =
  {
    super.onCreateOptionsMenu(menu)
    if (useMenu) {
      val inflater = getMenuInflater
      if (iFrame > 0) {
        inflater.inflate(R.menu.walkthrough_menu_etc, menu)
      } else {
        inflater.inflate(R.menu.walkthrough_menu_0, menu)
      }
      true
    } else {
      false
    }
  }

  override def onOptionsItemSelected(item : MenuItem) =
  {
    item.getItemId match {
      case R.id.slide1 => replaceSlide(1)
      case R.id.slide2 => replaceSlide(2)
      case R.id.slide3 => replaceSlide(3)
      case R.id.slide4 => replaceSlide(4)
      case R.id.slide5 => replaceSlide(5)
      case R.id.slide6 => replaceSlide(6)
      case _ => finish
    }
    true
  }

  private def replaceSlide(iSlide : Int)
  {
    val intent = new Intent(this, classOf[LeashWalkthroughActivity])
    intent.putExtra("iFrame", iSlide)
    intent.putExtra("menu", useMenu)
    startActivity(intent)
  }

  override def onTouch(v : View, event : MotionEvent) : Boolean =
  {
    event.getAction match {
      case MotionEvent.ACTION_DOWN => {
        x1 = event.getX
        true
      }
      case MotionEvent.ACTION_UP => {
        val x2 = event.getX
        val delta = x2 - x1
        if (Math.abs(delta) > 100) {
          if (delta < 0) {
            // swipe right
            if (iFrame < (frames.size - 1)) {
              iFrame += 1
              updateFrame
            }
          } else {
            // swipe left
            if (iFrame > 0) {
              iFrame -= 1
              updateFrame
            }
          }
        }
        true
      }
      case _ =>
        false
    }
  }

  private def updateFrame()
  {
    frameImg.setImageResource(frames(iFrame))
    textView.setText(textArray(iFrame))
    if (iFrame >= (frames.size - 1)) {
      buttonView.setVisibility(View.VISIBLE)
      hintView.setVisibility(View.GONE)
    } else {
      buttonView.setVisibility(View.GONE)
      hintView.setVisibility(View.VISIBLE)
    }
    setTitle(getString(R.string.walkthrough_title) +
      " (" + (iFrame + 1) + "/7)")
    invalidateOptionsMenu
    LeashAnalytics.trackScreen("Walkthrough " + iFrame)
  }

  private def loadAnimation =
  {
    val img = findView(TR.walkthrough_animation_image)
    img.setOnTouchListener(this)
    img
  }

  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.walkthrough)
    iFrame = getIntent.getIntExtra("iFrame", 0)
  }

  override protected def onResume()
  {
    super.onResume
    updateFrame
  }

  override protected def startNextActivity()
  {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val editor = prefs.edit
    editor.putBoolean(LeashSettingsActivity.PREF_WALKTHROUGH, true)
    editor.apply
    val intent = new Intent(this, classOf[LeashControlActivity])
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    finish
    startActivity(intent)
  }
}
