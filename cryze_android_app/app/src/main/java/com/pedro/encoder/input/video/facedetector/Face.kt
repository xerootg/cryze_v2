/*
 * Copyright (C) 2024 pedroSG94.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pedro.encoder.input.video.facedetector

import android.graphics.Point
import android.graphics.Rect

/**
 * Created by pedro on 10/10/23.
 */
data class Face(
  val id: Int?, //depend if device support it, if not supported the value could be -1
  val leftEye: Point?, //depend if device support it
  val rightEye: Point?, //depend if device support it
  val mouth: Point?, //depend if device support it
  val rect: Rect,
  val score: Int //range 1 to 100
)
