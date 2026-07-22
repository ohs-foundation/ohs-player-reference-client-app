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

import dev.ohs.fhir.model.r4.terminologies.ResourceType
import dev.ohs.fhir.sync.download.ResourceParamsBasedDownloadWorkManager

/**
 * In-memory implementation of [ResourceParamsBasedDownloadWorkManager.TimestampContext]. Tracks
 * per-[ResourceType] download cursors only for the lifetime of the process — a fresh app launch
 * re-downloads all resources for each configured type rather than only what changed since the last
 * session. The user-visible last-synced timestamp is unaffected: it is tracked separately by
 * [dev.ohs.fhir.sync.FhirDataStore].
 */
class InMemoryTimestampContext : ResourceParamsBasedDownloadWorkManager.TimestampContext {
  private val lastUpdatedByResourceType = mutableMapOf<ResourceType, String>()

  override suspend fun saveLastUpdatedTimestamp(resourceType: ResourceType, timestamp: String?) {
    if (timestamp != null) {
      lastUpdatedByResourceType[resourceType] = timestamp
    }
  }

  override suspend fun getLasUpdateTimestamp(resourceType: ResourceType): String? =
    lastUpdatedByResourceType[resourceType]
}
