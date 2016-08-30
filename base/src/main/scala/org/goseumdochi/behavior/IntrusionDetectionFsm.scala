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

package org.goseumdochi.behavior

import org.goseumdochi.common._
import org.goseumdochi.common.MoreMath._
import org.goseumdochi.control._
import org.goseumdochi.vision._

import akka.actor._

import scala.concurrent.duration._

object IntrusionDetectionFsm
{
  sealed trait State
  sealed trait Data

  // received messages
  // * ControlActor.CameraAcquiredMsg
  // * ControlActor.BodyMovedMsg
  // * MotionDetector.MotionDetectedMsg
  // * VisionActor.TheaterClickMsg

  // sent messages
  // * ControlActor.UseVisionAnalyzersMsg
  // * ControlActor.ActuateMoveMsg

  // states
  case object Blind extends State
  case object WaitingForIntruder extends State

  // data
  case object Empty extends Data
  case object Panicky extends Data
  final case class BodyAt(
    pos : PlanarPos
  ) extends Data
  final case class Paused(
    expiration : TimePoint
  ) extends Data
}
import IntrusionDetectionFsm._
import MotionDetector._

class IntrusionDetectionFsm()
    extends BehaviorFsm[State, Data]
{
  private val pausePeriod = settings.IntrusionDetection.pausePeriod

  startWith(Blind, Empty)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg(_, eventTime), _) => {
      sender ! ControlActor.UseVisionAnalyzersMsg(Seq(
        settings.BodyRecognition.className,
        settings.IntrusionDetection.motionClassName),
        eventTime)
      goto(WaitingForIntruder)
    }
  }

  when(WaitingForIntruder) {
    case Event(VisionActor.TheaterClickMsg(pos, retinalPos, eventTime), _) => {
      self ! MotionDetectedMsg(
        pos,
        RetinalPos(retinalPos.x - 10, retinalPos.y - 10),
        RetinalPos(retinalPos.x + 10, retinalPos.y + 10),
        eventTime)
      stay
    }
    case Event(msg : MotionDetectedMsg, BodyAt(bodyPos)) => {
      val eventTime = msg.eventTime
      val intruderPos = msg.pos
      // HALF_PI adjusts for screen orientation
      val theta = normalizeRadiansPositive(
        polarMotion(bodyPos, intruderPos).theta + HALF_PI)
      var heading = ((theta * 12) / TWO_PI).toInt
      if (heading == 0) {
        heading = 12
      }
      recordObservation("INTRUDER", eventTime, Seq(heading))
      sender ! ControlActor.ActuateMoveMsg(
        bodyPos, intruderPos, settings.Motor.defaultSpeed,
        0.seconds, eventTime)
      goto(WaitingForIntruder) using Paused(eventTime + pausePeriod)
    }
    case Event(msg : MotionDetectedMsg, _) => {
      stay
    }
    case Event(msg : ControlActor.BodyMovedMsg, Paused(expiration)) => {
      if (msg.eventTime > expiration) {
        stay using BodyAt(msg.pos)
      } else {
        stay
      }
    }
    case Event(msg : ControlActor.BodyMovedMsg, Panicky) => {
      stay using Paused(msg.eventTime + pausePeriod)
    }
    case Event(msg : ControlActor.BodyMovedMsg, _) => {
      stay using BodyAt(msg.pos)
    }
  }

  whenUnhandled {
    case Event(msg : ControlActor.PanicAttackMsg, _) => {
      goto(WaitingForIntruder) using Panicky
    }
    case event => handleUnknown(event)
  }

  initialize()
}
