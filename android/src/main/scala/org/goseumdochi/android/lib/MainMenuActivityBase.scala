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

import org.goseumdochi.android.common._

import android.view._

trait MainMenuActivityBase extends ActivityBase
{
  override def onCreateOptionsMenu(menu : Menu) =
  {
    super.onCreateOptionsMenu(menu)
    val inflater = getMenuInflater
    inflater.inflate(R.menu.main_menu, menu)
    true
  }

  override def onOptionsItemSelected(item : MenuItem) =
  {
    val itemId = item.getItemId
    if (itemId == R.id.about) {
      startAboutActivity
      true
    } else if (itemId == R.id.help) {
      startHelpActivity
      true
    } else if (itemId == R.id.bugs) {
      startBugsActivity
      true
    } else if (itemId == R.id.walkthrough) {
      startWalkthroughActivity
      true
    } else if (itemId == R.id.settings) {
      startSettingsActivity
      true
    } else {
      false
    }
  }

  protected def startAboutActivity()

  protected def startHelpActivity()

  protected def startBugsActivity()

  protected def startSettingsActivity()

  protected def startWalkthroughActivity()
  {
  }
}
