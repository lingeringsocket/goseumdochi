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

import android._
import android.app._
import android.bluetooth._
import android.content._
import android.content.pm._
import android.net._
import android.os._
import android.preference._
import android.view._
import android.widget._

import java.util._

class MainActivity extends Activity
{
  private final val PERMISSION_REQUEST = 42

  private final val ENABLE_BT_REQUEST = 43

  private var bluetoothEnabled = false

  private var cameraEnabled = false

  private var locationEnabled = false

  private var startRequested = false

  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    setContentView(R.layout.main)
    requestPrerequisites
  }

  private def requestPrerequisites()
  {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter
    if (!bluetoothAdapter.isEnabled) {
      val intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
      startActivityForResult(intent, ENABLE_BT_REQUEST)
    } else {
      bluetoothEnabled = true
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      cameraEnabled = hasCameraPermission
      locationEnabled = hasLocationPermission
      val gotPermissions = cameraEnabled && locationEnabled
      if (!gotPermissions) {
        val permissions = new ArrayList[String]
        if (!cameraEnabled) {
          permissions.add(Manifest.permission.CAMERA)
        }
        if (!locationEnabled) {
          permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        requestPermissions(
          permissions.toArray(
            new Array[String](permissions.size)),
          PERMISSION_REQUEST)
      }
    } else {
      cameraEnabled = true
      locationEnabled = true
    }
  }

  override protected def onActivityResult(
    requestCode : Int, resultCode : Int, intent : Intent)
  {
    if (requestCode == ENABLE_BT_REQUEST) {
      if (resultCode == Activity.RESULT_OK) {
        bluetoothEnabled = true
        tryStartControl
      } else {
        toastLong("Watchdog cannot run without Bluetooth enabled. ")
      }
    }
  }

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

  def onStartClicked(v : View)
  {
    startRequested = true
    if (!tryStartControl) {
      requestPrerequisites
    }
  }

  private def tryStartControl() =
  {
    if (startRequested && bluetoothEnabled && cameraEnabled && locationEnabled)
    {
      val intent = new Intent(this, classOf[ControlActivity])
      startActivity(intent)
      true
    } else {
      false
    }
  }

  private def hasCameraPermission =
    hasPermission(Manifest.permission.CAMERA)

  private def hasLocationPermission =
    hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

  private def hasPermission(permission : String) =
  {
    (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) ||
      (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
  }

  override def onRequestPermissionsResult(
    requestCode : Int, permissions : Array[String], grantResults : Array[Int])
  {
    if (requestCode == PERMISSION_REQUEST) {
      for (i <- 0 until permissions.length) {
        if (grantResults(i) != PackageManager.PERMISSION_GRANTED) {
          toastLong(
            "Watchdog cannot run until all requested permissions " +
              "have been granted.")
          return
        }
        permissions(i) match {
          case Manifest.permission.ACCESS_COARSE_LOCATION =>
            locationEnabled = true
            tryStartControl
          case Manifest.permission.CAMERA =>
            cameraEnabled = true
            tryStartControl
          case _ =>
        }
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  private def toastLong(msg : String)
  {
    Toast.makeText(getApplicationContext, msg, Toast.LENGTH_LONG).show
  }
}
