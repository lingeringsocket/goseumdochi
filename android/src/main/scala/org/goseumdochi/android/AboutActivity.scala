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
import android.os._
import android.view._

class AboutActivity extends Activity with View.OnClickListener
{
  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.about)

    val okButton = findViewById(R.id.about_ok)
    okButton.setOnClickListener(this)
    val projectUrl = findViewById(R.id.project_url)
    projectUrl.setOnClickListener(this)
  }

  def onClick(v : View)
  {
    v.getId match {
      case R.id.about_ok => finish
      case R.id.project_url => openProjectUrlInBrowser
      case _ =>
    }
  }

  private def openProjectUrlInBrowser()
  {
    val uri = Uri.parse(getString(R.string.about_project_url))
    val intent = new Intent(Intent.ACTION_VIEW, uri)
    startActivity(intent)
  }
}
