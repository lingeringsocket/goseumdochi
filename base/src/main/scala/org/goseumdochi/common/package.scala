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

package org.goseumdochi

import scala.concurrent.duration._
import org.bytedeco.javacpp.opencv_core._

package object common
{
  type TimeSpan = FiniteDuration
  val TimeSpan = FiniteDuration
  type LightColor = CvScalar

  def resourcePath(resource : String) : String =
    classOf[TimePoint].getResource(resource).getPath
}
