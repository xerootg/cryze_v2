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

package com.pedro.rtsp.utils

import java.nio.ByteBuffer
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun ByteArray.encodeToString(): String {
  return Base64.encode(this)
}

fun ByteBuffer.getData(): ByteArray {
  val startCodeSize = this.getVideoStartCodeSize()
  val bytes = ByteArray(this.capacity() - startCodeSize)
  this.position(startCodeSize)
  this.get(bytes, 0, bytes.size)
  return bytes
}

/**
 * Sets a long value into a byte array starting from a specified index.
 *
 * @param n The long value to set.
 * @param begin The starting index in the byte array.
 * @param end The ending index in the byte array.
 */
fun ByteArray.setLong(n: Long, begin: Int, end: Int) {
  var value = n
  for (i in end - 1 downTo begin step 1) {
    // Set the byte at index i to the least significant byte of value
    this[i] = (value % 256).toByte()
    // Right shift value by 8 bits to process the next byte
    value = value shr 8
  }
}

fun ByteBuffer.getVideoStartCodeSize(): Int {
  var startCodeSize = 0
  if (this.get(0).toInt() == 0x00 && this.get(1).toInt() == 0x00
    && this.get(2).toInt() == 0x00 && this.get(3).toInt() == 0x01) {
    //match 00 00 00 01
    startCodeSize = 4
  } else if (this.get(0).toInt() == 0x00 && this.get(1).toInt() == 0x00
    && this.get(2).toInt() == 0x01) {
    //match 00 00 01
    startCodeSize = 3
  }
  return startCodeSize
}