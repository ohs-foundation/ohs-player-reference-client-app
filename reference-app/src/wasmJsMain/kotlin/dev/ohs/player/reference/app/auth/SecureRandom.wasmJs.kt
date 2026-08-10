/*
 * Copyright 2026 Open Health Stack Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package dev.ohs.player.reference.app.auth

import org.khronos.webgl.Int8Array
import org.khronos.webgl.get

private fun fillRandom(array: Int8Array): Unit = js("crypto.getRandomValues(array)")

/** Browser CSPRNG via Web Crypto `crypto.getRandomValues`. */
internal actual fun secureRandomBytes(size: Int): ByteArray {
  val array = Int8Array(size)
  fillRandom(array)
  return ByteArray(size) { array[it] }
}
