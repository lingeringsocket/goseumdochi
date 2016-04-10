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
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacv._

import com.typesafe.config._

import java.awt.event._

object CaptureMain extends App
{
  val config = ConfigFactory.load()
  val settings = Settings(config)

  captureFrameOnClick()

  def captureOneFrame(outFileName : String)
  {
    val videoStream =
      settings.instantiateObject(settings.Vision.cameraClass).
        asInstanceOf[VideoStream]
    videoStream.beforeNext
    val img = OpenCvUtil.convert(grabOneFrame(videoStream))
    videoStream.afterNext
    cvSaveImage(outFileName, img)
  }

  def captureFrameOnClick()
  {
    val canvas = new CanvasFrame("Webcam")
    canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)
    val videoStream =
      settings.instantiateObject(settings.Vision.cameraClass).
        asInstanceOf[VideoStream]
    val running = true
    var capture = false
    var nextSuffix = 1

    canvas.getCanvas.addMouseListener(new MouseAdapter {
      override def mouseClicked(e : MouseEvent)
      {
        capture = true
      }
    })

    println("Click mouse inside webcam window to capture; " +
      "close webcam window to quit")
    while (running) {
      videoStream.beforeNext
      val frame = grabOneFrame(videoStream)
      canvas.showImage(frame)
      if (capture) {
        val img = OpenCvUtil.convert(frame)
        val outFileName = "frame" + nextSuffix + ".jpg"
        nextSuffix += 1
        cvSaveImage(outFileName, img)
        capture = false
        println("Captured " + outFileName)
        img.release
      }
      videoStream.afterNext
    }
  }

  private def grabOneFrame(videoStream : VideoStream) =
    videoStream.nextFrame._1
}
