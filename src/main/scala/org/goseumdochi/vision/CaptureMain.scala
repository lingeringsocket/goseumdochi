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

import org.bytedeco.javacpp.opencv_imgcodecs._

import com.typesafe.config._

import java.io._

object CaptureMain extends App with RetinalTheaterListener
{
  private val config = ConfigFactory.load()

  private val settings = Settings(config)

  private var capture = false

  private var running = true

  captureFrameOnClick()

  def captureFrameOnClick()
  {
    val videoFileName = "video.mp4"
    val canvasTheater = new CanvasTheater
    val fileTheater = new VideoFileTheater(new File(videoFileName))
    val theater = new TeeTheater(Seq(canvasTheater, fileTheater))
    theater.setListener(this)
    val retinalInput =
      settings.instantiateObject(settings.Vision.inputClass).
        asInstanceOf[RetinalInput]
    var nextSuffix = 1

    println("Capturing video to " + videoFileName)
    println("Click mouse inside webcam window to capture still image; " +
      "close webcam window to quit")
    while (running) {
      val (frame, frameTime) = retinalInput.nextFrame
      theater.display(frame, frameTime)
      if (capture) {
        val img = retinalInput.frameToImage(frame)
        val outFileName = "frame" + nextSuffix + ".jpg"
        nextSuffix += 1
        cvSaveImage(outFileName, img)
        capture = false
        println("Captured " + outFileName)
        img.release
      }
    }
    println("Flushing video to storage")
    fileTheater.quit
    println("Video saved successfully")
  }

  override def onTheaterClick(retinalPos : RetinalPos)
  {
    capture = true
  }

  override def onTheaterClose()
  {
    running = false
  }
}
