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

package org.goseumdochi.view.fx

import org.goseumdochi.common._
import org.goseumdochi.control._
import org.goseumdochi.perception._
import org.goseumdochi.vision._
import org.goseumdochi.view._

import scalafx.Includes._
import scalafx.application._
import scalafx.animation._
import scalafx.geometry._
import scalafx.scene._
import scalafx.scene.paint._
import scalafx.scene.shape._
import scalafx.scene.chart._
import scalafx.util.StringConverter

import javafx.event._
import javafx.embed.swing.JFXPanel

import scala.concurrent._
import scala.concurrent.duration._

case class BodyRetinalPos(
  pos : RetinalPos,
  eventTime : TimePoint
)

class RetinalView(settings : Settings) extends PerceptualView
{
  override def processHistory(events : Seq[PerceptualEvent])
  {
    RetinalView.createTimeline(events, settings.View.playbackRate)
    RetinalView.startVisualization()
  }

  override def processEvent(event : PerceptualEvent)
  {
    RetinalView.processEvent(event)
  }

  override def close()
  {
    RetinalView.close()
  }
}

object RetinalView
{
  private var bottomRightOpt : Option[RetinalPos] = None

  private var retinalTransform : RetinalTransform = IdentityRetinalTransform

  private[fx] def bottomRight = bottomRightOpt.getOrElse(RetinalPos(100, 100))

  private[fx] val body = new Circle {
    centerX = 0
    centerY = 0
    radius = 20
    fill = Color.LightBlue
    visible = false
  }

  private[fx] val timeKnob = new Rectangle {
    x = 0
    y = 0
    width = 10
    height = 10
    fill = Color.Red
  }

  private[fx] var timeline : Option[Timeline] = None

  private val timelineCompletion = Promise[ActionEvent]

  private def startVisualization()
  {
    new JFXPanel
    new Thread(new Runnable() {
      override def run() = {
        RetinalViewWindow.main(Array[String]())
      }
    }).start
  }

  private def processEvent(event : PerceptualEvent)
  {
    RetinalView.filterEvent(event, true) match {
      case Some(BodyRetinalPos(pos, eventTime)) => {
        Platform.runLater({
          body.centerX = pos.x
          body.centerY = pos.y
          body.visible = true
        })
      }
      case _ =>
    }
  }

  private def filterEvent(event : PerceptualEvent, immediate : Boolean)
      : Option[BodyRetinalPos] =
  {
    event.msg match {
      case VisionActor.DimensionsKnownMsg(bottomRight, _) => {
        bottomRightOpt = Some(bottomRight)
        if (immediate) {
          startVisualization()
        }
        None
      }
      case ControlActor.CalibratedMsg(_, xform, _) => {
        retinalTransform = xform
        None
      }
      case msg : ControlActor.BodyMovedMsg => Some(
        BodyRetinalPos(retinalTransform.worldToRetina(msg.pos), msg.eventTime))
      case _ => None
    }
  }

  private def close()
  {
    timeline.foreach(t =>
      Await.result(timelineCompletion.future, Duration.Inf))
  }

  private def createTimeline(
    events : Seq[PerceptualEvent], playbackRate : Double)
  {
    val maxMillis = events.map(_.msg.eventTime.d.toMillis).max
    val newTimeline = new Timeline {
      rate = playbackRate
      keyFrames = events.flatMap(filterEvent(_, false)).map(brp => {
        val millis = brp.eventTime.d.toMillis
        at (scalafx.util.Duration(millis)) {
          Set(
            body.centerX -> brp.pos.x,
            body.centerY -> brp.pos.y,
            body.visible -> true,
            timeKnob.x -> (millis.toDouble/maxMillis)*bottomRight.x
          )
        }
      })
    }
    newTimeline.setOnFinished(new EventHandler[ActionEvent] {
      def handle(event : ActionEvent) {
        timelineCompletion.success(event)
      }
    })
    timeline = Some(newTimeline)
  }
}

// upside down y-axis hack comes from
// http://stackoverflow.com/questions/18026706/chart-with-inverted-y-axis

object RetinalViewWindow extends JFXApp
{
  private val retinaWidth = RetinalView.bottomRight.x

  private val retinaHeight = RetinalView.bottomRight.y

  stage = new JFXApp.PrimaryStage {
    title.value = "Perceptual View (retinal frame of reference)"
    width = retinaWidth
    height = retinaHeight
    scene = new Scene {
      fill = Color.Black
      content = Seq(
        new NumberAxis {
          side = Side.TOP
          label = "Retina Pixels X"
          lowerBound = 0
          upperBound = retinaWidth
          minWidth = retinaWidth
          autoRanging = false
          tickUnit = 100
        },
        new NumberAxis {
          side = Side.LEFT
          label = "Retina Pixels Y"
          lowerBound = -retinaHeight
          upperBound = 0
          minHeight = retinaHeight
          autoRanging = false
          tickUnit = 100
          tickLabelFormatter = StringConverter.toStringConverter(num =>
            (-(num.intValue)).toString
          )
        },
        RetinalView.body
      ) ++ {
        RetinalView.timeline.map(timeline =>
          new Group {
            translateY = retinaHeight - 100
            children = Seq(
              new NumberAxis {
                side = Side.BOTTOM
                label = "Milliseconds"
                lowerBound = 0
                upperBound = timeline.totalDuration.get.toMillis
                minWidth = retinaWidth
                autoRanging = false
                tickUnit = 1000
              },
              RetinalView.timeKnob
            )
          }
        )
      }
    }
  }

  RetinalView.timeline.foreach(_.play)
}
