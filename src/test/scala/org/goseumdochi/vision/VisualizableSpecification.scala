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

import org.specs2.mutable._

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacv._

import com.typesafe.config._

import akka.actor._
import akka.testkit._

import java.util.concurrent.atomic._

abstract class VisualizableSpecification(confFile : String = "test.conf")
    extends Specification
{
  protected val settings = Settings(loadConfig(confFile))

  protected def shouldVisualize = settings.Test.visualize

  private var canvas : Option[CanvasFrame] = None

  protected def loadConfig(overrideConf : String) =
  {
    val actualConf = {
      if (overrideConf.isEmpty) {
        confFile
      } else {
        overrideConf
      }
    }
    if (actualConf.isEmpty) {
      ConfigFactory.load
    } else {
      ConfigFactory.load(actualConf)
    }
  }

  protected def visualize(img : IplImage)
  {
    if (!shouldVisualize) {
      return
    }
    val c = loadCanvas
    c.showImage(OpenCvUtil.convert(img))
    while (c.waitKey(-1) == null) {
    }
  }

  protected def visualize(img : IplImage, pos : RetinalPos)
  {
    if (!shouldVisualize) {
      return
    }

    val center = OpenCvUtil.point(pos)
    cvCircle(img, center, 2, AbstractCvScalar.BLACK, 6, CV_AA, 0)

    visualize(img)
  }

  protected def visualize(
    img : IplImage, pos : PlanarPos,
    xform : RetinalTransform = IdentityRetinalTransform)
  {
    visualize(img, xform.worldToRetina(pos))
  }

  private def loadCanvas() =
  {
    canvas match {
      case Some(canvasFrame) => {
        canvasFrame
      }
      case _ => {
        val canvasFrame = new CanvasFrame("Test Visualization")
        canvasFrame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)
        canvas = Some(canvasFrame)
        canvasFrame
      }
    }
  }

  protected def configureSystem(overrideConf : String) =
    ActorSystem(
      "TestActors_" + VisualizableSpecification.suffixGenerator.incrementAndGet,
      loadConfig(overrideConf))

  abstract class VisualizableActorExample(overrideConf : String)
      extends TestKit(configureSystem(overrideConf))
  {
    protected val settings = ActorSettings(system)
  }
}

object VisualizableSpecification
{
  private val suffixGenerator = new AtomicLong
}
