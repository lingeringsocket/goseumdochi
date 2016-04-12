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

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.helper.opencv_core._

import akka.actor._
import akka.routing._

import scala.concurrent.duration._

object VisionActor
{
  // sent messages
  final case class DimensionsKnownMsg(
    corner : RetinalPos, eventTime : TimePoint)
      extends EventMsg
  trait ObjDetectedMsg extends EventMsg

  // internal messages
  final case class GrabFrameMsg(lastTime : TimePoint)

  // received messages
  final case class ActivateAnalyzersMsg(
    analyzerClassNames : Seq[String],
    xform : RetinalTransform)
  final case class HintBodyLocationMsg(pos : PlanarPos, eventTime : TimePoint)
      extends EventMsg
}
import VisionActor._

class VisionActor(videoStream : VideoStream, theater : RetinalTheater)
    extends Actor with Listeners
{
  private val settings = ActorSettings(context)

  private val throttlePeriod = settings.Vision.throttlePeriod

  private var analyzers : Seq[VisionAnalyzer] = Seq.empty

  private var lastImg : Option[IplImage] = None

  private var lastGray : Option[IplImage] = None

  private var cornerSeen = false

  private var hintBodyPos : Option[PlanarPos] = None

  private var retinalTransform : RetinalTransform = FlipRetinalTransform

  private var shutDown = false

  private val converter = OpenCvUtil.newConverter

  def receive =
  {
    case GrabFrameMsg(lastTime) => {
      if (!shutDown) {
        val thisTime = TimePoint.now
        val analyze = (thisTime > lastTime + throttlePeriod)
        grabOne(analyze)
        import context.dispatcher
        context.system.scheduler.scheduleOnce(200.milliseconds) {
          self ! GrabFrameMsg(if (analyze) thisTime else lastTime)
        }
      }
    }
    case ActivateAnalyzersMsg(analyzerClassNames, xform) => {
      retinalTransform = xform
      analyzers = analyzerClassNames.map(
        settings.instantiateObject(_, xform).
          asInstanceOf[VisionAnalyzer])
    }
    case HintBodyLocationMsg(pos, eventTime) => {
      hintBodyPos = Some(pos)
    }
    case m : Any => {
      listenerManagement(m)
    }
  }

  private def analyzeFrame(img : IplImage, frameTime : TimePoint)
  {
    val gray = OpenCvUtil.grayscale(img)
    val copy = cvCloneImage(img)

    lastGray.foreach(
      prevGray => {
        val prevImg = lastImg.get
        analyzers.map(
          analyzer => {
            analyzer.analyzeFrame(
              img, prevImg, gray, prevGray, frameTime, hintBodyPos).
              foreach(msg => {
                msg match {
                  case BodyDetector.BodyDetectedMsg(pos, _) => {
                    hintBodyPos = Some(pos)
                  }
                  case _ => {}
                }
                gossip(msg)
              }
            )
          }
        )
        prevGray.release
        prevImg.release
      }
    )
    lastGray = Some(gray)
    lastImg = Some(copy)
  }

  private def grabOne(analyze : Boolean)
  {
    try {
      videoStream.beforeNext()
      val (frame, frameTime) = videoStream.nextFrame()
      val img = converter.convert(frame)
      if (!cornerSeen) {
        val corner = RetinalPos(img.width, img.height)
        gossip(DimensionsKnownMsg(corner, frameTime))
        cornerSeen = true
      }
      if (analyze) {
        analyzeFrame(img, frameTime)
      } else {
        hintBodyPos match {
          case Some(pos) => {
            val center = OpenCvUtil.point(retinalTransform.worldToRetina(pos))
            cvCircle(img, center, 2, AbstractCvScalar.GREEN, 6, CV_AA, 0)
          }
          case _ => {}
        }
      }
      val converted = converter.convert(img)
      theater.display(converted)
      img.release
    } catch {
      case ex : Throwable => {
        ex.printStackTrace
      }
    } finally {
      videoStream.afterNext()
    }
  }

  override def preStart()
  {
    theater.setActor(this)
    self ! GrabFrameMsg(TimePoint.now)
  }

  override def postStop()
  {
    if (!shutDown) {
      shutDown = true
      videoStream.quit
      theater.quit
    }
  }

  def onTheaterClick(retinalPos : RetinalPos)
  {
    gossip(
      MotionDetector.MotionDetectedMsg(
        retinalTransform.retinaToWorld(retinalPos),
        TimePoint.now))
  }

  def onTheaterClose()
  {
    if (!shutDown) {
      shutDown = true
      context.system.shutdown
    }
  }
}
