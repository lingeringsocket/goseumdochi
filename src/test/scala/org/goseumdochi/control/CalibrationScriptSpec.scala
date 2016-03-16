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

package org.goseumdochi.control

class CalibrationScriptSpec
    extends ScriptedSpecification
{
  isolated

  "script" should
  {
    "orient and then doze" in new AkkaExample("projective-test.conf")
    {
      runScript(system, "/scripted/doze.json")
    }

    "orient and then draw squares" in new AkkaExample("square-test.conf")
    {
      runScript(system, "/scripted/square.json")
    }
  }
}
