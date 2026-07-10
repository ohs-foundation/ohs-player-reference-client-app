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

import java.io.File

private const val SNAPSHOT_FILE_NAME = "fhir-repository.json"

actual object PlatformRepositorySnapshotStore : RepositorySnapshotStore {
  actual override suspend fun read(): String? =
    snapshotFile().takeIf(File::exists)?.readText().takeUnless { it.isNullOrBlank() }

  actual override suspend fun write(snapshot: String) {
    val file = snapshotFile()
    file.parentFile?.mkdirs()
    file.writeText(snapshot)
  }

  private fun snapshotFile(): File {
    val userHome = System.getProperty("user.home").orEmpty().ifBlank { "." }
    return File(File(userHome, ".ohs-player-reference-app"), SNAPSHOT_FILE_NAME)
  }
}
