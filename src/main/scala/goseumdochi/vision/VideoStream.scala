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

package goseumdochi.vision

import goseumdochi.common._

import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacv._

import scala.concurrent.duration._

trait VideoStream
{
  def beforeNext() {}

  def nextFrame() : (IplImage, TimePoint)

  def afterNext() {}

  def quit() {}
}

class LocalVideoStream(settings : Settings) extends VideoStream
{
  private val frameGrabber = startGrabber

  private def startGrabber() =
  {
    val grabber = new OpenCVFrameGrabber(0)
    grabber.setBitsPerPixel(CV_8U)
    grabber.setImageMode(FrameGrabber.ImageMode.COLOR)
    // FIXME: find a way to suppress "HIGHGUI ERROR: V4L: Property
    // <unknown property string>(16) not supported by device"
    grabber.start
    grabber
  }

  override def nextFrame() =
  {
    val img = OpenCvUtil.convert(frameGrabber.grab)
    cvFlip(img, img, 1)
    (img, TimePoint.now)
  }

  override def quit()
  {
    frameGrabber.stop
  }
}

class RemoteVideoStream(settings : Settings) extends VideoStream
{
  private var frameGrabber : Option[IPCameraFrameGrabber] = None

  override def beforeNext()
  {
    val url = settings.Vision.remoteCameraUrl
    if (url.isEmpty) {
      Settings.complainMissing("goseumdochi.vision.remote-camera-url")
    }
    val grabber = new IPCameraFrameGrabber(url)
    grabber.setBitsPerPixel(CV_8U)
    grabber.setImageMode(FrameGrabber.ImageMode.COLOR)
    grabber.start
    frameGrabber = Some(grabber)
  }

  override def nextFrame() =
  {
    (OpenCvUtil.convert(frameGrabber.get.grab), TimePoint.now)
  }

  override def afterNext()
  {
    frameGrabber.get.stop
    frameGrabber = None
  }

  override def quit()
  {
  }
}

class PlaybackStream(
  keyFrames : Seq[(String, TimeSpan)], realTime : Boolean = true)
    extends VideoStream
{
  private val circular = Iterator.continually(keyFrames).flatten

  private var simulatedTime = TimePoint(TimeSpan(1, SECONDS))

  override def nextFrame() =
  {
    val (filename, delay) = circular.next
    val frameTime = {
      if (realTime) {
        Thread.sleep(delay.toMillis)
        TimePoint.now
      } else {
        simulatedTime += delay
        simulatedTime
      }
    }
    val img = OpenCvUtil.convert(cvLoadImage(filename))
    (OpenCvUtil.convert(img), frameTime)
  }
}
