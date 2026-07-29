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

import dev.ohs.fhir.engine.sync.SyncJobStatus

/** In-memory [SyncManager] that counts calls, so ViewModel tests can assert sync behavior. */
internal class FakeSyncManager(
  private val syncResult: suspend () -> SyncJobStatus = { SyncJobStatus.Succeeded() }
) : SyncManager {
  var syncNowCount = 0
    private set

  var cancelSyncNowCount = 0
    private set

  var startPeriodicCount = 0
    private set

  var cancelPeriodicCount = 0
    private set

  override suspend fun syncNow(): SyncJobStatus {
    syncNowCount++
    return syncResult()
  }

  override suspend fun cancelSyncNow() {
    cancelSyncNowCount++
  }

  override suspend fun startPeriodicSync() {
    startPeriodicCount++
  }

  override suspend fun cancelPeriodicSync() {
    cancelPeriodicCount++
  }
}
