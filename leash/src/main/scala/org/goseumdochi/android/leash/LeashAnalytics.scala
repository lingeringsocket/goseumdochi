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

import android.app._
import android.content._
import android.preference._

import com.google.android.gms.analytics._

import java.io._

object LeashAnalytics
{
  private var ga : Option[GoogleAnalytics] = None

  private lazy val tracker = ga.get.newTracker(R.xml.global_tracker)

  def init(app : Application)
  {
    val instance = GoogleAnalytics.getInstance(app)
    ga = Some(instance)
    val reporter = new ExceptionReporter(
      tracker, Thread.getDefaultUncaughtExceptionHandler, app)
    reporter.setExceptionParser(new LeashAnalyticsExceptionParser(app))
    Thread.setDefaultUncaughtExceptionHandler(reporter)
    tracker.enableAutoActivityTracking(false)
    tracker.enableExceptionReporting(true)
    val prefs = PreferenceManager.getDefaultSharedPreferences(app)
    if (prefs.getBoolean(LeashSettingsActivity.PREF_ANALYTICS_OPT_OUT, false)) {
      instance.setAppOptOut(true)
    }
  }

  def trackScreen(screenName : String)
  {
    if (optOut) {
      return
    }
    tracker.setScreenName(screenName)
    tracker.send((new HitBuilders.ScreenViewBuilder).build)
  }

  def trackEvent(actionName : String, label : String)
  {
    if (optOut) {
      return
    }
    tracker.send(
      (new HitBuilders.EventBuilder).
        setCategory("RollWithMe").setAction(actionName).setLabel(label).build)
  }

  def setOptOut(optOut : Boolean)
  {
    ga.foreach(_.setAppOptOut(optOut))
  }

  private def optOut = ga.map(_.getAppOptOut).getOrElse(false)
}

class LeashAnalyticsExceptionParser(context : Context)
    extends StandardExceptionParser(context, null)
{
  override protected def getDescription(
    cause : Throwable,
    element : StackTraceElement,
    threadName : String) =
  {
    val descriptionBuilder = new StringBuilder
    val writer = new StringWriter
    val printWriter = new PrintWriter(writer)
    cause.printStackTrace(printWriter)
    descriptionBuilder.append(writer.toString)
    printWriter.close
    descriptionBuilder.toString
  }
}
