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

import org.goseumdochi.android.common.R

import android._
import android.app._
import android.bluetooth._
import android.content._
import android.content.pm._
import android.os._
import android.view._

import java.util._

import com.getkeepsafe.relinker._

abstract class PrerequisitesActivityBase extends ActivityBase
{
  private final val ENABLE_BT_REQUEST = 43

  private var cameraEnabled = false

  private var locationEnabled = false

  private var startRequested = false

  override protected def onCreate(savedInstanceState : Bundle)
  {
    super.onCreate(savedInstanceState)
    ReLinker.recursively.loadLibrary(this, "jniopencv_core");
    ReLinker.recursively.loadLibrary(this, "opencv_core");
    ReLinker.recursively.loadLibrary(this, "jniopencv_imgcodecs");
    ReLinker.recursively.loadLibrary(this, "opencv_imgcodecs");
    ReLinker.recursively.loadLibrary(this, "jniopencv_imgproc");
    ReLinker.recursively.loadLibrary(this, "opencv_imgproc");
  }

  protected def checkPrerequisites()
  {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      cameraEnabled = hasCameraPermission
      locationEnabled = hasLocationPermission
    } else {
      cameraEnabled = true
      locationEnabled = true
    }
  }

  protected def requestPrerequisites()
  {
    if (!bluetoothEnabled) {
      val intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
      startActivityForResult(intent, ENABLE_BT_REQUEST)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      checkPrerequisites
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
    }
  }

  override protected def onActivityResult(
    requestCode : Int, resultCode : Int, intent : Intent)
  {
    if (requestCode == ENABLE_BT_REQUEST) {
      if (resultCode == Activity.RESULT_OK) {
        tryStart
      } else {
        toastLong(R.string.toast_bluetooth_required)
      }
    }
  }

  private def bluetoothEnabled = BluetoothAdapter.getDefaultAdapter.isEnabled

  private def allPrerequisitesMet =
    bluetoothEnabled && cameraEnabled && locationEnabled

  protected def startNextActivity()

  protected def tryStart() =
  {
    if (startRequested && allPrerequisitesMet) {
      startNextActivity
      true
    } else {
      false
    }
  }

  def onStartClicked(v : View)
  {
    startRequested = true
    if (!tryStart) {
      requestPrerequisites
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
            tryStart
          case Manifest.permission.CAMERA =>
            cameraEnabled = true
            tryStart
          case _ =>
        }
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }
}

abstract class MainActivityBase
    extends PrerequisitesActivityBase with MainMenuActivityBase
{
}
