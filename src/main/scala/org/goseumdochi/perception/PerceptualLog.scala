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

import org.goseumdochi.common._

import java.io._
import scala.io._
import resource._

import scala.concurrent.duration._

import org.json4s._
import org.json4s.native._

object PerceptualLog
{
  implicit val formats =
    Serialization.formats(
      FullTypeHints(List(classOf[EventMsg], classOf[RetinalTransform]))) +
      TimeSpanSerializer + TimePointSerializer

  def read(filePath : String) : Seq[PerceptualEvent] =
    managed(Source.fromFile(filePath)).acquireAndGet(src => {
      Serialization.read[Array[PerceptualEvent]](src.getLines.mkString)
    })

  def write(event : PerceptualEvent) =
    Serialization.writePretty(event)
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

object TimePointSerializer extends Serializer[TimePoint]
{
  private val TimePointClass = classOf[TimePoint]

  def deserialize(implicit format: Formats)
      : PartialFunction[(TypeInfo, JValue), TimePoint] =
  {
    case (TypeInfo(TimePointClass, _), json) => json match {
      case JObject(JField("millis", JInt(millis)) :: _) =>
        TimePoint(TimeSpan(millis.toLong, MILLISECONDS))
      case x => throw new MappingException(
        "Can't convert " + x + " to TimePoint")
    }
  }

  def serialize(implicit formats: Formats)
      : PartialFunction[Any, JValue] =
  {
    case x: TimePoint =>
      import JsonDSL._
      ("millis" -> x.d.toMillis)
  }
}

object TimeSpanSerializer extends Serializer[TimeSpan]
{
  private val TimeSpanClass = classOf[TimeSpan]

  def deserialize(implicit format: Formats)
      : PartialFunction[(TypeInfo, JValue), TimeSpan] =
  {
    case (TypeInfo(TimeSpanClass, _), json) => json match {
      case JObject(JField("millis", JInt(millis)) :: _) =>
        TimeSpan(millis.toLong, MILLISECONDS)
      case x => throw new MappingException(
        "Can't convert " + x + " to TimeSpan")
    }
  }

  def serialize(implicit formats: Formats)
      : PartialFunction[Any, JValue] =
  {
    case x: TimeSpan =>
      import JsonDSL._
      ("millis" -> x.toMillis)
  }
}
