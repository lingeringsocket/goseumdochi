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

package org.goseumdochi.view.opencv

import org.goseumdochi.control._
import org.goseumdochi.common._
import org.goseumdochi.vision._

import org.bytedeco.javacv._

import com.typesafe.config._

object PerspectiveMain extends App
{
  val config = ConfigFactory.load()
  val settings = Settings(config)

  showPerspective

  def showPerspective()
  {
    val (xform, bodyMapping) = ControlActor.readOrientation(settings)
    val perspective = xform.asInstanceOf[RestrictedPerspectiveTransform]

    val canvas = new CanvasFrame("Webcam")
    canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)
    val retinalInput =
      settings.instantiateObject(settings.Vision.inputClass).
        asInstanceOf[RetinalInput]
    val running = true

    println("Close webcam window to quit")
    while (running) {
      retinalInput.beforeNext
      val frame = grabOneFrame(retinalInput)
      val img = OpenCvUtil.convert(frame)
      perspective.visualize(img)
      canvas.showImage(OpenCvUtil.convert(img))
      img.release
      retinalInput.afterNext
    }
  }

  private def grabOneFrame(retinalInput : RetinalInput) =
    retinalInput.nextFrame._1
}
