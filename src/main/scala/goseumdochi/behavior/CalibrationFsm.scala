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

package goseumdochi.behavior

import goseumdochi.common._
import goseumdochi.control._
import goseumdochi.vision._

import akka.actor._
import akka.pattern._
import akka.util._

import scala.concurrent.duration._

import goseumdochi.common.MoreMath._

object CalibrationFsm
{
  sealed trait State
  sealed trait Data

  // received messages
  // * ControlActor.CameraAcquiredMsg
  // * ControlActor.BodyMovedMsg

  // sent messages
  // * VisionActor.ActivateAnalyzersMsg
  // * ControlActor.CalibratedMsg
  // * ControlActor.ActuateImpulseMsg

  // states
  case object Blind extends State
  case object WaitingForStart extends State
  case object WaitingForEnd extends State

  // data
  case object Uninitialized extends Data
  final case class StartPoint(pos : PlanarPos) extends Data
}
import CalibrationFsm._

class CalibrationFsm()
    extends BehaviorFsm[State, Data]
{
  private val settings = Settings(context)

  private val impulse = PolarImpulse(settings.Motor.defaultSpeed, 0.8, 0)

  startWith(Blind, Uninitialized)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg, _) => {
      sender ! VisionActor.ActivateAnalyzersMsg(Seq(
        settings.BodyRecognition.className))
      goto(WaitingForStart)
    }
    case Event(ControlActor.BodyMovedMsg(_, _), _) => {
      stay
    }
  }

  when(WaitingForStart) {
    case Event(ControlActor.BodyMovedMsg(pos, eventTime), _) => {
      sender ! ControlActor.ActuateImpulseMsg(impulse, eventTime)
      goto(WaitingForEnd) using StartPoint(pos)
    }
  }

  when(WaitingForEnd) {
    case Event(
      ControlActor.BodyMovedMsg(endPos, eventTime),
      StartPoint(startPos)) =>
    {
      val predictedMotion = predictMotion(impulse)
      val actualMotion = polarMotion(startPos, endPos)
      val bodyMapping = BodyMapping(
        actualMotion.distance / predictedMotion.distance,
        normalizeRadians(actualMotion.theta - predictedMotion.theta))
      sender ! ControlActor.CalibratedMsg(bodyMapping)
      goto(Blind)
    }
  }

  initialize()
}
