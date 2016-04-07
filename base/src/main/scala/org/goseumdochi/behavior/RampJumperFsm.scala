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
import org.goseumdochi.control._
import org.goseumdochi.vision._

import akka.actor._

import scala.concurrent.duration._
import org.goseumdochi.common.MoreMath._

object RampJumperFsm
{
  sealed trait State
  sealed trait Data

  // received messages
  // * ControlActor.CameraAcquiredMsg
  // * ControlActor.BodyMovedMsg
  // * RampDetector.RampDetectedMsg

  // sent messages
  // * ControlActor.UseVisionAnalyzersMsg
  // * ControlActor.ActuateMoveMsg

  // states
  case object Blind extends State
  case object WaitingForRamp extends State
  case object ManeuveringToLaunch extends State
  case object Launched extends State

  // data
  case object Empty extends Data
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
  private val settings = ActorSettings(context)

  startWith(Blind, Empty)

  when(Blind) {
    case Event(ControlActor.CameraAcquiredMsg(_, eventTime), _) => {
      sender ! ControlActor.UseVisionAnalyzersMsg(Seq(
        settings.BodyRecognition.className,
        classOf[RampDetector].getName),
        eventTime)
      goto(WaitingForRamp)
    }
  }

  when(WaitingForRamp) {
    case Event(RampDetectedMsg(ramp, _), _) => {
      goto(ManeuveringToLaunch) using LaunchTrajectory(ramp, None)
    }
    case Event(msg : ControlActor.BodyMovedMsg, _) => {
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
          pos, ramp.center, settings.Motor.fullSpeed, 1.second, eventTime)
        goto(Launched) using Empty
      } else {
        val extraTime = {
          if (offset.distance < 40.0) {
            200.milliseconds
          } else {
            0.seconds
          }
        }
        sender ! ControlActor.ActuateMoveMsg(
          pos, launchPos, settings.Motor.defaultSpeed, extraTime, eventTime)
        stay using newTrajectory
      }
    }
    case Event(msg : RampDetectedMsg, _) => {
      stay
    }
  }

  when(Launched, stateTimeout = 5.seconds) {
    case Event(StateTimeout, _) => {
      goto(WaitingForRamp)
    }
    case Event(msg : ControlActor.BodyMovedMsg, _) => {
      stay
    }
    case Event(msg : RampDetectedMsg, _) => {
      stay
    }
  }

  whenUnhandled {
    case Event(msg : ControlActor.PanicAttackMsg, _) => {
      goto(WaitingForRamp) using Empty
    }
    case event => handleUnknown(event)
  }

  initialize()
}
