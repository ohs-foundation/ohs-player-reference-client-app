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
package dev.ohs.player.reference.app.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.ohs.fhir.engine.sync.createDataStore
import dev.ohs.player.reference.app.auth.AndroidAppContext

private val dataStore: DataStore<Preferences> by lazy {
  createDataStore {
    AndroidAppContext.application.filesDir.resolve(SYNC_TIMESTAMP_DATASTORE_FILE_NAME).absolutePath
  }
}

internal actual fun createSyncTimestampDataStore(): DataStore<Preferences> = dataStore
