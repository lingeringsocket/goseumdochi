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

import org.goseumdochi.vision._

import android.app._
import android.content._
import android.media._
import android.net._
import android.os._

import java.util._
import java.text._
import java.io._

import scala.util._
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.concurrent._

object GlobalVideo extends ContextBase
{
  private var appContextOpt : Option[Context] = None

  private var mainActivityOpt : Option[Activity] = None

  private val sdfFilename = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

  private val pending = new TrieMap[Future[Object], Object]

  override def getApplicationContext() = appContextOpt.get

  def init(appContext : Context, mainActivity : Activity)
  {
    appContextOpt = Some(appContext)
    mainActivityOpt = Some(mainActivity)
  }

  def createVideoFileTheater() : VideoFileTheater =
  {
    val movies = Environment.getExternalStoragePublicDirectory(
      Environment.DIRECTORY_MOVIES)
    val dir = new File(
      movies, getApplicationContext.getString(R.string.app_name))
    dir.mkdirs
    val file = new File(
      dir, sdfFilename.format(Calendar.getInstance.getTime) + ".mkv")
    new VideoFileTheater(file)
  }

  def closeTheater(theater : VideoFileTheater)
  {
    toastLong(R.string.toast_saving_video)
    implicit val execContext = ExecutionContext.fromExecutor(
      AsyncTask.THREAD_POOL_EXECUTOR)
    val saveAttempt = Future {
      theater.quit
      val path = theater.getFile.getAbsolutePath
      val scanPromise = Promise[Object]()
      val scanFuture = scanPromise.future
      val scanListener = new MediaScannerConnection.OnScanCompletedListener {
        override def onScanCompleted(path : String, uri : Uri)
        {
          pending.remove(scanFuture)
          scanPromise.success(path)
          runUi {
            () => toastLong(R.string.toast_video_saved)
          }
        }
      }
      MediaScannerConnection.scanFile(
        getApplicationContext,
        Array(path), null, scanListener)
      pending.put(scanFuture, path)
      theater
    }
    pending.put(saveAttempt, theater)
    saveAttempt.onComplete {
      case t : Try[Object] => {
        pending.remove(saveAttempt)
        if (t.isFailure) {
          runUi {
            () => toastLong(R.string.toast_video_save_failed)
          }
        }
      }
    }
  }

  private def runUi(fn : () => Unit)
  {
    mainActivityOpt.foreach(_.runOnUiThread(new Runnable {
      def run()
      {
        fn()
      }
    }))
  }

  def shutdown()
  {
    pending.keys.foreach(Await.ready(_, Duration.Inf))
    pending.clear
    appContextOpt = None
    mainActivityOpt = None
  }
}
