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

import android.app._
import android.content._
import android.content.pm._
import android.os._
import android.widget._

import android.support.v7.app._

trait ContextBase
{
  def getApplicationContext() : Context

  protected def toastLong(resId : Int)
  {
    toastLong(getApplicationContext.getString(resId))
  }

  protected def toastLong(msg : String)
  {
    Toast.makeText(getApplicationContext, msg, Toast.LENGTH_LONG).show
  }
}

trait ActivityBaseNoCompat extends Activity with TypedFindView with ContextBase
{
  protected final val PERMISSION_REQUEST = 42

  protected def hasPermission(permission : String) =
  {
    (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) ||
    (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
  }
}

trait ActivityBase extends AppCompatActivity with ActivityBaseNoCompat
{
}

