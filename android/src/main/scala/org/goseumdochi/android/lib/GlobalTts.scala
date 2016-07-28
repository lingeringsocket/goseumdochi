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

package org.goseumdochi.android.lib

import android.content._
import android.speech.tts._

object GlobalTts
{
  private var appContextOpt : Option[Context] = None

  private var textToSpeech : Option[TextToSpeech] = None

  def init(appContext : Context, enable : Boolean)
  {
    appContextOpt = Some(appContext)
    if (enable) {
      start(appContext)
    }
  }

  private def start(appContext : Context)
  {
    var newTextToSpeech : TextToSpeech = null
    newTextToSpeech = new TextToSpeech(
      appContext, new TextToSpeech.OnInitListener {
        override def onInit(status : Int)
        {
          if (status != TextToSpeech.ERROR) {
            textToSpeech = Some(newTextToSpeech)
          }
        }
      })
  }

  def shutdown()
  {
    textToSpeech.foreach(t => {
      t.stop
      t.shutdown
    })
    textToSpeech = None
    appContextOpt = None
  }

  def reinit(enable : Boolean)
  {
    if (enable) {
      start(appContextOpt.get)
    } else {
      shutdown
    }
  }

  def speak(voiceMessage : String)
  {
    textToSpeech.foreach(_.speak(voiceMessage, TextToSpeech.QUEUE_ADD, null))
  }
}
