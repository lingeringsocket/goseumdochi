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

package goseumdochi.vision

import goseumdochi.common._

import org.specs2.mutable._

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacv._

import com.typesafe.config._

import akka.actor._

abstract class VisualizableSpecification(confFile : String = "simulation.conf")
    extends Specification
{
  protected val actorSystem = configureSystem()

  protected val settings = Settings(actorSystem)

  private var canvas : Option[CanvasFrame] = None

  protected def configureSystem() =
  {
    val systemName = "TestActors"
    if (confFile.isEmpty) {
      ActorSystem(systemName)
    } else {
      val chosenConfig = ConfigFactory.load(confFile)
      val defaultConfig = ConfigFactory.load()
      val config = chosenConfig.withFallback(defaultConfig)
      ActorSystem(systemName, config)
    }
  }

  protected def visualize(img : IplImage)
  {
    if (!shouldVisualize) {
      return
    }
    val canvas = loadCanvas
    canvas.showImage(OpenCvUtil.convert(img))
    while (canvas.waitKey(-1) == null) {
    }
  }

  protected def visualize(img : IplImage, pos : PlanarPos)
  {
    if (!shouldVisualize) {
      return
    }

    val center = OpenCvUtil.point(pos)
    cvCircle(img, center, 2, AbstractCvScalar.BLACK, 6, CV_AA, 0)

    visualize(img)
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

  protected def shouldVisualize = settings.Test.visualize
}
