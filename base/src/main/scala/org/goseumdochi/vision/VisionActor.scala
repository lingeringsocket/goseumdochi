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

import akka.actor._
import akka.routing._

import scala.concurrent.duration._

import collection._

object VisionActor
{
  // sent messages
  final case class DimensionsKnownMsg(
    corner : RetinalPos, eventTime : TimePoint)
      extends EventMsg
  trait AnalyzerResponseMsg extends EventMsg
  {
    def renderOverlay(overlay : RetinalOverlay)
    {}
  }
  trait ObjDetectedMsg extends AnalyzerResponseMsg
  final case class RequireLightMsg(
    color : LightColor,
    eventTime : TimePoint)
      extends AnalyzerResponseMsg

  // internal messages
  final case class GrabFrameMsg(lastTime : TimePoint)

  // received messages
  final case class ActivateAnalyzersMsg(
    analyzerClassNames : Seq[String],
    xform : RetinalTransform)
  final case class HintBodyLocationMsg(pos : PlanarPos, eventTime : TimePoint)
      extends EventMsg

  def startFrameGrabber(visionActor : ActorRef, listener : ActorRef)
  {
    visionActor ! Listen(listener)
    visionActor ! GrabFrameMsg(TimePoint.now)
  }
}
import VisionActor._

class VisionActor(retinalInput : RetinalInput, theater : RetinalTheater)
    extends Actor with Listeners
{
  private val settings = ActorSettings(context)

  private val throttlePeriod = settings.Vision.throttlePeriod

  private var analyzers : Seq[VisionAnalyzer] = Seq.empty

  private var lastImg : Option[IplImage] = None

  private var lastGray : Option[IplImage] = None

  private var corner : Option[RetinalPos] = None

  private var hintBodyPos : Option[PlanarPos] = None

  private var retinalTransform : RetinalTransform = FlipRetinalTransform

  private var shutDown = false

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
      closeAnalyzers
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

  private def analyzeFrame(img : IplImage, frameTime : TimePoint) =
  {
    val gray = OpenCvUtil.grayscale(img)
    val copy = img.clone
    val msgs = new mutable.ArrayBuffer[VisionActor.AnalyzerResponseMsg]

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
                msgs += msg
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
    msgs
  }

  private def grabOne(analyze : Boolean)
  {
    try {
      retinalInput.beforeNext()
      val (frame, frameTime) = retinalInput.nextFrame()
      val converted = OpenCvUtil.convert(frame)
      // without this, Android crashes...wish I understood why!
      val img = converted.clone
      if (corner.isEmpty) {
        val newCorner = RetinalPos(img.width, img.height)
        gossip(DimensionsKnownMsg(newCorner, frameTime))
        corner = Some(newCorner)
      }
      val overlay = new OpenCvRetinalOverlay(img, retinalTransform, corner.get)
      if (analyze) {
        val msgs = analyzeFrame(img, frameTime)
        msgs.foreach(_.renderOverlay(overlay))
      } else {
        hintBodyPos match {
          case Some(pos) => {
            overlay.drawCircle(
              retinalTransform.worldToRetina(pos),
              6, NamedColor.GREEN, 2)
          }
          case _ => {}
        }
      }
      val result = OpenCvUtil.convert(img)
      theater.display(result)
      img.release
      converted.release
    } catch {
      case ex : Throwable => {
        ex.printStackTrace
      }
    } finally {
      retinalInput.afterNext()
    }
  }

  override def preStart()
  {
    theater.setActor(this)
  }

  override def postStop()
  {
    if (!shutDown) {
      shutDown = true
      retinalInput.quit
      theater.quit
    }
    closeAnalyzers
  }

  private def closeAnalyzers()
  {
    analyzers.foreach(_.close)
    analyzers = Seq.empty
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
