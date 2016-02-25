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

import scala.concurrent.duration._
import goseumdochi.common.MoreMath._

object RampJumperFsm
{
  sealed trait State
  sealed trait Data

  // received messages
  // * ControlActor.CameraAcquiredMsg
  // * ControlActor.BodyMovedMsg
  // * RampDetector.RampDetectedMsg

  // sent messages
  // * VisionActor.ActivateAnalyzersMsg
  // * ControlActor.ActuateMoveMsg

  // states
  case object Blind extends State
  case object WaitingForRamp extends State
  case object ManeuveringToLaunch extends State
  case object Launched extends State

  // data
  case object Uninitialized extends Data
  final case class LaunchTrajectory(
    ramp : OrientedRamp,
    launchPos : Option[PlanarPos]
  ) extends Data
}
import RampJumperFsm._
import RampDetector._

class RampJumperFsm()
    extends BehaviorFsm[State, Data]
{
  private val settings = Settings(context)

  startWith(Blind, Uninitialized)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg, _) => {
      sender ! VisionActor.ActivateAnalyzersMsg(Seq(
        settings.BodyRecognition.className,
        classOf[RampDetector].getName))
      goto(WaitingForRamp)
    }
  }

  when(WaitingForRamp) {
    case Event(RampDetectedMsg(ramp, _), _) => {
      goto(ManeuveringToLaunch) using LaunchTrajectory(ramp, None)
    }
    case Event(ControlActor.BodyMovedMsg(_, _), _) => {
      stay
    }
  }

  when(ManeuveringToLaunch) {
    case Event(
      ControlActor.BodyMovedMsg(pos, eventTime),
      trajectory : LaunchTrajectory) =>
    {
      val ramp = trajectory.ramp
      val newTrajectory = trajectory.launchPos match {
        case Some(launchPos) => {
          trajectory
        }
        case _ => {
          val center = ramp.center
          val centerMotion = polarMotion(pos, center)
          val entryMotion = polarMotion(pos, ramp.entry)
          val radius = polarMotion(center, ramp.entry)
          val extendedRadius = {
            if (entryMotion.distance > centerMotion.distance) {
              PolarVector(-7*radius.distance, radius.theta)
            } else {
              PolarVector(7*radius.distance, radius.theta)
            }
          }
          val dest = PlanarPos(
            center.x + extendedRadius.dx, center.y + extendedRadius.dy)
          LaunchTrajectory(ramp, Some(dest))
        }
      }
      val launchPos = newTrajectory.launchPos.get
      val offset = polarMotion(pos, launchPos)
      if (offset.distance < 15.0) {
        sender ! ControlActor.ActuateMoveMsg(
          pos, ramp.center, settings.Motor.fullSpeed, 1.0, eventTime)
        goto(Launched) using Uninitialized
      } else {
        val extraTime = {
          if (offset.distance < 40.0) {
            0.2
          } else {
            0.0
          }
        }
        sender ! ControlActor.ActuateMoveMsg(
          pos, launchPos, settings.Motor.defaultSpeed, extraTime, eventTime)
        stay using newTrajectory
      }
    }
    case Event(RampDetectedMsg(_, _), _) => {
      stay
    }
  }

  when(Launched, stateTimeout = 5.seconds) {
    case Event(StateTimeout, _) => {
      goto(WaitingForRamp)
    }
    case Event(ControlActor.BodyMovedMsg(_, _) | RampDetectedMsg(_, _), _) => {
      stay
    }
  }

  whenUnhandled {
    case Event(ControlActor.PanicAttack, _) => {
      goto(WaitingForRamp) using Uninitialized
    }
    case event => handleUnknown(event)
  }

  initialize()
}
