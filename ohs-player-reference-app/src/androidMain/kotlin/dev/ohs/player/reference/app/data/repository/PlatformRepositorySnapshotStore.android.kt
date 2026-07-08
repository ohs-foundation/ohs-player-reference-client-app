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

import android.content.Context
import java.io.File

private const val SNAPSHOT_FILE_NAME = "fhir-repository.json"

internal object AndroidAppContextHolder {
  var applicationContext: Context? = null
}

actual object PlatformRepositorySnapshotStore : RepositorySnapshotStore {
  override suspend fun read(): String? =
    snapshotFile().takeIf(File::exists)?.readText().takeUnless { it.isNullOrBlank() }

  override suspend fun write(snapshot: String) {
    val file = snapshotFile()
    file.parentFile?.mkdirs()
    file.writeText(snapshot)
  }

  private fun snapshotFile(): File {
    val contextDirectory = AndroidAppContextHolder.applicationContext?.filesDir
    val fallbackDirectory = File(System.getProperty("java.io.tmpdir").orEmpty().ifBlank { "." })
    return File(contextDirectory ?: fallbackDirectory, SNAPSHOT_FILE_NAME)
  }
}
