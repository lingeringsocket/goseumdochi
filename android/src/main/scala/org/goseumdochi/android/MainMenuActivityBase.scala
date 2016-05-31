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

import android.app._
import android.content._
import android.net._
import android.view._

class MainMenuActivityBase extends Activity with TypedFindView
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
    item.getItemId match {
      case R.id.about =>
        startActivity(new Intent(this, classOf[AboutActivity]))
        true
      case R.id.help =>
        val uri = Uri.parse(getString(R.string.help_url))
        startActivity(new Intent(Intent.ACTION_VIEW, uri))
        true
      case R.id.settings =>
        startActivity(new Intent(this, classOf[SettingsActivity]))
        true
      case _ =>
        false
    }
  }
}
