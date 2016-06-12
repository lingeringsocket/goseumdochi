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
import android.os._
import android.preference._
import android.text.method._
import android.view._

import java.util._

class MainActivity extends MainMenuActivityBase
{
  private final val ENABLE_BT_REQUEST = 43

  private var bluetoothEnabled = false

  private var cameraEnabled = false

  private var locationEnabled = false

  private var setupRequested = false

  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    setContentView(R.layout.main)
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val enableVoice = prefs.getBoolean(
      SettingsActivity.PREF_ENABLE_VOICE, true)
    if (enableVoice) {
      GlobalTts.init(getApplicationContext)
    }
    GlobalVideo.init(getApplicationContext, this)
    findView(TR.cctv_text).setMovementMethod(LinkMovementMethod.getInstance)
    requestPrerequisites
  }

  override protected def onDestroy()
  {
    GlobalTts.shutdown
    GlobalVideo.shutdown
    super.onDestroy
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
        toastLong(R.string.toast_bluetooth_required)
      }
    }
  }

  def onSetupClicked(v : View)
  {
    setupRequested = true
    if (!tryStartControl) {
      requestPrerequisites
    }
  }

  private def allPrerequisitesMet =
    bluetoothEnabled && cameraEnabled && locationEnabled

  private def tryStartControl() =
  {
    if (setupRequested && allPrerequisitesMet) {
      val intent = new Intent(this, classOf[SetupActivity])
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
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

  override def onRequestPermissionsResult(
    requestCode : Int, permissions : Array[String], grantResults : Array[Int])
  {
    if (requestCode == PERMISSION_REQUEST) {
      for (i <- 0 until permissions.length) {
        if (grantResults(i) != PackageManager.PERMISSION_GRANTED) {
          toastLong(R.string.toast_permissions_required)
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
}
