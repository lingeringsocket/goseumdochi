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

package org.goseumdochi.android

import android.content._
import android.speech.tts._

object GlobalTts
{
  private var textToSpeech : Option[TextToSpeech] = None

  def init(appContext : Context)
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
  }

  def speak(voiceMessage : String)
  {
    textToSpeech.foreach(_.speak(voiceMessage, TextToSpeech.QUEUE_ADD, null))
  }
}
