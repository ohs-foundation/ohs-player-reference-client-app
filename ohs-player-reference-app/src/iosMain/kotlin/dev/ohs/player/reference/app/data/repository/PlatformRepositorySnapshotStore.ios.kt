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
package dev.ohs.player.reference.app.data.repository

import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask
import platform.posix.SEEK_END
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fwrite
import platform.posix.rewind

private const val SNAPSHOT_FILE_NAME = "fhir-repository.json"

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
actual object PlatformRepositorySnapshotStore : RepositorySnapshotStore {
  actual override suspend fun read(): String? {
    val file = fopen(snapshotPath(), "rb") ?: return null
    return try {
      fseek(file, 0, SEEK_END)
      val size = ftell(file)
      if (size <= 0L) return null
      rewind(file)

      val bytes = ByteArray(size.toInt())
      val bytesRead =
        bytes.usePinned { pinned ->
          fread(pinned.addressOf(0), 1u, bytes.size.toULong(), file).toInt()
        }
      bytes.decodeToString(endIndex = bytesRead).takeIf { it.isNotBlank() }
    } finally {
      fclose(file)
    }
  }

  actual override suspend fun write(snapshot: String) {
    val file = fopen(snapshotPath(), "wb") ?: return
    try {
      val bytes = snapshot.encodeToByteArray()
      bytes.usePinned { pinned -> fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), file) }
    } finally {
      fclose(file)
    }
  }

  private fun snapshotPath(): String {
    val directory =
      (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String) ?: NSTemporaryDirectory()
    return "$directory/$SNAPSHOT_FILE_NAME"
  }
}
