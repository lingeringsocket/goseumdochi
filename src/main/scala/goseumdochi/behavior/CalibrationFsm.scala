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

import scala.math._
import goseumdochi.common.MoreMath._

import scala.concurrent.duration._

object CalibrationFsm
{
  sealed trait State
  sealed trait Data

  // received messages
  // * ControlActor.CameraAcquiredMsg
  // * ControlActor.BodyMovedMsg
  // * MotionDetector.MotionDetectedMsg

  // sent messages
  // * VisionActor.ActivateAnalyzersMsg
  // * VisionActor.HintBodyLocationMsg
  // * ControlActor.CalibratedMsg
  // * ControlActor.ActuateImpulseMsg

  // states
  case object Blind extends State
  case object WaitingForQuiet extends State
  case object FindingBody extends State
  case object WaitingForStart extends State
  case object WaitingForEnd extends State

  // data
  case object Empty extends Data
  final case class WithControl(controlActor : ActorRef) extends Data
  final case class StartPoint(pos : PlanarPos) extends Data
}
import CalibrationFsm._

class CalibrationFsm()
    extends BehaviorFsm[State, Data]
{
  private val settings = Settings(context)

  private val forwardImpulse =
    PolarImpulse(settings.Motor.defaultSpeed, 0.8, 0)

  private val backwardImpulse =
    PolarImpulse(settings.Motor.defaultSpeed, 0.8, Pi)

  startWith(Blind, Empty)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg, _) => {
      sender ! VisionActor.ActivateAnalyzersMsg(Seq(
        settings.BodyRecognition.className,
        classOf[FineMotionDetector].getName))
      goto(WaitingForQuiet) using WithControl(sender)
    }
  }

  when(WaitingForQuiet,
    stateTimeout = Duration(settings.Calibration.quietPeriod, MILLISECONDS))
  {
    case Event(StateTimeout, WithControl(controlActor)) => {
      controlActor ! ControlActor.ActuateImpulseMsg(
        backwardImpulse, System.currentTimeMillis)
      goto(FindingBody)
    }
    case _ => {
      stay
    }
  }

  // FIXME:  add a StateTimeout for moving around more aggressively
  when(FindingBody) {
    case Event(MotionDetector.MotionDetectedMsg(pos, eventTime), _) => {
      sender ! VisionActor.HintBodyLocationMsg(pos)
      goto(WaitingForStart)
    }
  }

  when(WaitingForStart) {
    case Event(msg : MotionDetector.MotionDetectedMsg, _) => {
      stay
    }
    case Event(ControlActor.BodyMovedMsg(pos, eventTime), _) => {
      sender ! ControlActor.ActuateImpulseMsg(forwardImpulse, eventTime)
      goto(WaitingForEnd) using StartPoint(pos)
    }
  }

  when(WaitingForEnd) {
    case Event(
      ControlActor.BodyMovedMsg(endPos, eventTime),
      StartPoint(startPos)) =>
    {
      val predictedMotion = predictMotion(forwardImpulse)
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
