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

package com.google.uicd.backend.controllers.responses;

import com.google.auto.value.AutoValue;

/** Contains image in the form of Base64 string */
@AutoValue
public abstract class ImageResponse {
  public abstract String getImage();

  public static ImageResponse create(String base64Img) {
    return new AutoValue_ImageResponse(base64Img);
  }
}
