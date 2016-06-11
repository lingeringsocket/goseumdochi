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
    xform : RetinalTransform,
    eventTime : TimePoint)
      extends EventMsg
  final case class ActivateAugmentersMsg(
    augmenterClassNames : Seq[String],
    eventTime : TimePoint)
      extends EventMsg
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
    extends Actor with Listeners with RetinalTheaterListener
{
  private val settings = ActorSettings(context)

  private val throttlePeriod = settings.Vision.throttlePeriod

  private var analyzers : Seq[VisionAnalyzer] = Seq.empty

  private var augmenters : Seq[VisionAugmenter] = Seq.empty

  private val imageDeck = new ImageDeck

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
    case ActivateAnalyzersMsg(analyzerClassNames, xform, eventTime) => {
      closeAnalyzers
      retinalTransform = xform
      analyzers = analyzerClassNames.map(
        settings.instantiateObject(_, xform).
          asInstanceOf[VisionAnalyzer])
    }
    case ActivateAugmentersMsg(augmenterClassNames, eventTime) => {
      closeAugmenters
      augmenters = augmenterClassNames.map(
        settings.instantiateObject(_).
          asInstanceOf[VisionAugmenter])
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
    val copy = img.clone
    val allMsgs = new mutable.ArrayBuffer[VisionActor.AnalyzerResponseMsg]

    imageDeck.cycle(copy)
    if (imageDeck.isReady) {
      analyzers.map(
        analyzer => {
          val analyzerMsgs = analyzer.analyzeFrame(
            imageDeck, frameTime, hintBodyPos)
          analyzerMsgs.foreach(msg => {
            msg match {
              case BodyDetector.BodyDetectedMsg(pos, _) => {
                hintBodyPos = Some(pos)
              }
              case _ => {}
            }
            gossip(msg)
            allMsgs += msg
          })
        }
      )
    }
    allMsgs
  }

  private def grabOne(analyze : Boolean)
  {
    try {
      val (frame, frameTime) = retinalInput.nextFrame
      val converted = retinalInput.frameToImage(frame)
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
      augmenters.foreach(_.augmentFrame(overlay, frameTime, hintBodyPos))
      val result = theater.imageToFrame(img)
      theater.display(result, frameTime)
      img.release
      converted.release
    } catch {
      case ex : Throwable => {
        ex.printStackTrace
      }
    }
  }

  override def preStart()
  {
    theater.setListener(this)
  }

  override def postStop()
  {
    if (!shutDown) {
      shutDown = true
      retinalInput.quit
      theater.quit
    }
    closeAnalyzers
    closeAugmenters
  }

  private def closeAnalyzers()
  {
    analyzers.foreach(_.close)
    analyzers = Seq.empty
  }

  private def closeAugmenters()
  {
    augmenters.foreach(_.close)
    augmenters = Seq.empty
  }

  override def onTheaterClick(retinalPos : RetinalPos)
  {
    gossip(
      MotionDetector.MotionDetectedMsg(
        retinalTransform.retinaToWorld(retinalPos),
        RetinalPos(retinalPos.x - 10, retinalPos.y - 10),
        RetinalPos(retinalPos.x + 10, retinalPos.y + 10),
        TimePoint.now))
  }

  override def onTheaterClose()
  {
    if (!shutDown) {
      shutDown = true
      context.system.shutdown
    }
  }
}
