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

import java.awt._
import java.awt.event._
import javax.swing._

import org.bytedeco.javacv._
import org.bytedeco.javacv.{Frame => CvFrame}

class CanvasTheater extends RetinalTheater
{
  private val canvasFrame = initCanvasFrame()

  override def display(frame : CvFrame, frameTime : TimePoint)
  {
    canvasFrame.showImage(frame)
  }

  override def quit()
  {
    Toolkit.getDefaultToolkit.getSystemEventQueue.postEvent(
      new WindowEvent(canvasFrame, WindowEvent.WINDOW_CLOSING))
  }

  private def initCanvasFrame() =
  {
    val cf = new CanvasFrame("Retina")
    cf.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    cf.getCanvas.addMouseListener(new MouseAdapter {
      override def mouseClicked(e : MouseEvent) {
        getListener.foreach(_.onTheaterClick(RetinalPos(e.getX, e.getY)))
      }
    })
    cf.addWindowListener(new WindowAdapter {
      override def windowClosing(e : WindowEvent)
      {
        super.windowClosing(e)
        getListener.foreach(_.onTheaterClose)
      }

      override def windowClosed(e : WindowEvent)
      {
        super.windowClosed(e)
      }
    })
    cf
  }
}
