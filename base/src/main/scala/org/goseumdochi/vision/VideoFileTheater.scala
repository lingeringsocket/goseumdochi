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

class VideoFileTheater(file : File) extends RetinalTheater
{
  private val SKIP_START = 5

  private var recorder : Option[FrameRecorder] = None

  private var firstSkipTime = TimePoint.ZERO

  private var firstFrameTime = TimePoint.ZERO

  private var skipped = 0

  private var stopped = false

  private def initRecorder(firstFrame : Frame) =
  {
    val r = new FFmpegFrameRecorder(
      file, firstFrame.imageWidth, firstFrame.imageHeight)
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
        r.record(frame)
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
      recorder = None
    }
  }

  def getFile = file
}
