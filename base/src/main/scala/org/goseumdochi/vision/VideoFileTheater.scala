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

package org.goseumdochi.vision

import org.goseumdochi.common._

import org.bytedeco.javacv._

import java.io._

class VideoFileTheater(file : File, filterString : String = "")
    extends RetinalTheater
{
  private val SKIP_START = 5

  private var recorder : Option[FrameRecorder] = None

  private var filter : Option[FrameFilter] = None

  private var firstSkipTime = TimePoint.ZERO

  private var firstFrameTime = TimePoint.ZERO

  private var skipped = 0

  private var stopped = false

  private def initRecorder(firstFrame : Frame) =
  {
    val width = firstFrame.imageWidth
    val height = firstFrame.imageHeight
    if (!filterString.isEmpty) {
      val f = new FFmpegFrameFilter("showinfo=n", width, height)
      f.start
      filter = Some(f)
    }
    val r = new FFmpegFrameRecorder(file, width, height)
    val skipTime = firstFrameTime - firstSkipTime
    val estimatedFrameRate =
      ((1000.0 * SKIP_START) / skipTime.toMillis.toDouble)
    val frameRate = {
      if (estimatedFrameRate < 2.0) {
        2
      } else {
        estimatedFrameRate.toInt
      }
    }
    r.setFormat(file.getName.split("\\.").last)
    r.setFrameRate(frameRate)
    r.start
    r
  }

  override def display(frame : Frame, frameTime : TimePoint)
  {
    this.synchronized {
      if (stopped) {
        return
      }
      if (skipped == 0) {
        firstSkipTime = frameTime
      }
      if (skipped < SKIP_START) {
        skipped += 1
        return
      }
      if (recorder.isEmpty) {
        firstFrameTime = frameTime
        recorder = Some(initRecorder(frame))
      }
      val ts = (frameTime - firstFrameTime).toMillis
      recorder.foreach(r => {
        // we don't bother with r.setTimestamp because last time I checked,
        // the implementation is bogus
        filter match {
          case Some(f) => {
            f.push(frame)
            var filteredFrame : Option[Frame] = None
            do {
              filteredFrame = Option(f.pull)
              filteredFrame.foreach(r.record(_))
            } while (!filteredFrame.isEmpty)
          }
          case _ => {
            r.record(frame)
          }
        }
      })
    }
  }

  override def quit()
  {
    this.synchronized {
      stopped = true
      recorder.foreach(r => {
        r.stop
        r.release
      })
      filter.foreach(f => {
        f.stop
        f.release
      })
      recorder = None
    }
  }

  def getFile = file
}
