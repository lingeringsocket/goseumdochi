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

import com.owlike.genson._
import com.owlike.genson.stream._
import com.owlike.genson.annotation._

import scala.concurrent.duration._

private[perception] class LogGensonBundle extends ScalaBundle
{
  override def configure(builder : GensonBuilder)
  {
    super.configure(builder)
    builder
      .useIndentation(true)
      .useClassMetadata(true)
      .useRuntimeType(true)
      .withConverters(TimePointConverter, TimeSpanConverter)
  }
}

private[perception] trait TimeConverter
{
  def serializeSpan(span : TimeSpan, writer : ObjectWriter)
  {
    writer.beginObject
    writer.writeName("millis").writeValue(span.toMillis)
    writer.endObject
  }

  def deserializeSpan(reader : ObjectReader) : TimeSpan =
  {
    var millis = 0L
    reader.beginObject
    while (reader.hasNext) {
      reader.next
      if (reader.name.equals("millis")) {
        millis = reader.valueAsLong
      }
    }
    reader.endObject
    TimeSpan(millis, MILLISECONDS)
  }
}

@HandleClassMetadata
private[perception] object TimePointConverter extends Converter[TimePoint]
    with TimeConverter
{
  override def serialize(point : TimePoint, writer : ObjectWriter, ctx : Context)
  {
    serializeSpan(point.d, writer)
  }

  override def deserialize(reader : ObjectReader, ctx : Context) : TimePoint =
  {
    TimePoint(deserializeSpan(reader))
  }
}

@HandleClassMetadata
private[perception] object TimeSpanConverter extends Converter[TimeSpan]
    with TimeConverter
{
  override def serialize(span : TimeSpan, writer : ObjectWriter, ctx : Context)
  {
    serializeSpan(span, writer)
  }

  override def deserialize(reader : ObjectReader, ctx : Context) : TimeSpan =
  {
    deserializeSpan(reader)
  }
}
