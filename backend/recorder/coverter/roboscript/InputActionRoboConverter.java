// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.uicd.backend.recorder.coverter.roboscript;

import com.google.uicd.backend.core.uicdactions.InputAction;
import com.google.uicd.backend.recorder.coverter.ActionConverter;

/** Implements the convert logic for click action to RoboScript */
public class InputActionRoboConverter implements ActionConverter<InputAction, RoboConvertContext> {

  @Override
  public boolean canConvert(InputAction action, RoboConvertContext context) {
    return false;
  }

  @Override
  public void convert(InputAction action, RoboConvertContext context) {
    if (!canConvert(action, context)) {
      return;
    }
    RoboAction roboAction = new RoboAction();
    roboAction.eventType = RoboEventType.VIEW_CLICKED;

    if (action.isSingleChar()) {
      // Most common action, make it more easy to read
      if (action.keyCode == 3) {
        roboAction.eventType = RoboEventType.GO_HOME;
      } else if (action.keyCode == 4) {
        roboAction.eventType = RoboEventType.PRESSED_BACK;
      } else {
        roboAction.eventType = RoboEventType.ENTER_TEXT;
      }

    } else {
      roboAction.eventType = RoboEventType.ENTER_TEXT;
    }
    context.addRoboAcction(roboAction);
  }
}
