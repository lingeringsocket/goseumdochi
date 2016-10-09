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

import android.content._
import android.graphics._
import android.hardware._
import android.preference._
import android.view._

import android.hardware.Camera

import scala.collection.JavaConverters._

class CameraPreview(
  context : Context, previewCallback : Camera.PreviewCallback)
    extends SurfaceView(context) with SurfaceHolder.Callback
{
  private val surfaceHolder = createHolder
  private var camera : Option[Camera] = None

  private def createHolder() =
  {
    val holder = getHolder
    holder.addCallback(this)
    holder
  }

  override def surfaceCreated(holder : SurfaceHolder)
  {
    val newCamera = Camera.open
    camera = Some(newCamera)
    try {
      newCamera.setPreviewDisplay(holder)
    } catch {
      case exception : Throwable => {
        newCamera.release
        camera = None
      }
    }
  }

  override def surfaceDestroyed(holder : SurfaceHolder)
  {
    closeCamera
  }

  def closeCamera()
  {
    this.synchronized {
      camera.foreach(c => {
        c.stopPreview
        c.release
      })
      camera = None
    }
  }

  override def surfaceChanged(
    holder : SurfaceHolder, format : Int,
    w : Int, h : Int)
  {
    camera.foreach(c => {
      val parameters = c.getParameters

      val sizes = parameters.getSupportedPreviewSizes.asScala
      val optimalSize =
        sizes.filter(s => (s.width <= w) && (s.height <= h)).maxBy(_.height)
      parameters.setPreviewSize(optimalSize.width, optimalSize.height)
      parameters.setFocusMode("continuous-video")

      val prefs = PreferenceManager.getDefaultSharedPreferences(context)
      try {
        parameters.setWhiteBalance(
          prefs.getString(SettingsActivity.PREF_WHITE_BALANCE,
            context.getString(R.string.pref_default_white_balance)))
      } catch {
        // probably we should be checking the list of supported
        // values instead
        case _ : Exception =>
      }

      c.setParameters(parameters)
      c.setPreviewCallbackWithBuffer(previewCallback)
      val size = parameters.getPreviewSize
      val data = new Array[Byte](
        size.width * size.height *
          ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8)
      c.addCallbackBuffer(data)
      c.startPreview
    })
  }
}
