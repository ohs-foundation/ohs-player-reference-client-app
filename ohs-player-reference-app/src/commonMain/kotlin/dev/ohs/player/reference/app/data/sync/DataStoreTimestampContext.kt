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
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.ohs.fhir.engine.sync.download.ResourceParamsBasedDownloadWorkManager
import dev.ohs.fhir.model.r4.terminologies.ResourceType
import kotlinx.coroutines.flow.first

/**
 * File name for the [DataStore] backing [DataStoreTimestampContext] — see
 * [createSyncTimestampDataStore].
 */
internal const val SYNC_TIMESTAMP_DATASTORE_FILE_NAME = "sync_timestamps.preferences_pb"

/** Supplies the platform [DataStore] backing [DataStoreTimestampContext]. */
internal expect fun createSyncTimestampDataStore(): DataStore<Preferences>

/**
 * Persists each [ResourceType]'s download cursor (`_lastUpdated`) in a Preferences [DataStore], so
 * a fresh app launch resumes incremental sync instead of re-downloading every configured resource
 * type from scratch. Mirrors kotlin-fhir-engine's engine-app `DemoDataStore`.
 */
class DataStoreTimestampContext(private val dataStore: DataStore<Preferences>) :
  ResourceParamsBasedDownloadWorkManager.TimestampContext {

  override suspend fun saveLastUpdatedTimestamp(resourceType: ResourceType, timestamp: String?) {
    if (timestamp == null) return
    dataStore.edit { prefs -> prefs[stringPreferencesKey(resourceType.name)] = timestamp }
  }

  override suspend fun getLasUpdateTimestamp(resourceType: ResourceType): String? =
    dataStore.data.first()[stringPreferencesKey(resourceType.name)]
}
