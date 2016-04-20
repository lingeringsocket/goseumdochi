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

package org.goseumdochi.perception

import java.io._
import scala.io._
import resource._

import com.owlike.genson._

object PerceptualLog
{
  private val genson = new ScalaGenson(
    new GensonBuilder().withBundle(new LogGensonBundle).create)

  def read(filePath : String) : Seq[PerceptualEvent] =
    managed(Source.fromFile(filePath)).acquireAndGet(src => {
      genson.fromJson[Array[PerceptualEvent]](src.getLines.mkString)
    })

  def write(event : PerceptualEvent) : String = genson.toJson(event)
}

class PerceptualLog(filePath : String) extends PerceptualProcessor
{
  private val pw = new PrintWriter(new File(filePath))

  private var first = true

  init()

  def init() {
    pw.println("[")
  }

  override def processHistory(
    events : Seq[PerceptualEvent])
  {
    events.foreach(event => processEvent(event))
  }

  override def processEvent(event : PerceptualEvent)
  {
    if (first) {
      first = false
    } else {
      pw.println(",")
    }
    pw.println(PerceptualLog.write(event))
    pw.flush
  }

  override def close()
  {
    pw.println("]")
    pw.close
  }
}
