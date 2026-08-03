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

import android.content.Context
import dev.ohs.fhir.engine.sync.CurrentSyncJobStatus
import dev.ohs.fhir.engine.sync.PeriodicSyncConfiguration
import dev.ohs.fhir.engine.sync.RepeatInterval
import dev.ohs.fhir.engine.sync.Sync
import dev.ohs.fhir.engine.sync.SyncJobStatus
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.first

/**
 * Android [SyncManager]: runs sync through WorkManager (via [AppFhirSyncWorker]) rather than in
 * process, so both one-time and periodic sync survive the triggering screen being backgrounded or
 * the process dying mid-sync. [PeriodicSyncConfiguration]'s default `SyncConstraints` already
 * requires `NetworkType.CONNECTED`, so WorkManager itself defers periodic runs until the device is
 * online.
 */
class WorkManagerSyncManager(private val context: Context) : SyncManager {
  override suspend fun syncNow(): SyncJobStatus {
    val terminalStatus =
      Sync.oneTimeSync<AppFhirSyncWorker>(context).first {
        it is CurrentSyncJobStatus.Succeeded ||
          it is CurrentSyncJobStatus.Failed ||
          it is CurrentSyncJobStatus.Cancelled
      }
    return when (terminalStatus) {
      is CurrentSyncJobStatus.Succeeded -> SyncJobStatus.Succeeded()
      else -> SyncJobStatus.Failed()
    }
  }

  override suspend fun cancelSyncNow() {
    Sync.cancelOneTimeSync<AppFhirSyncWorker>(context)
  }

  override suspend fun startPeriodicSync() {
    Sync.periodicSync<AppFhirSyncWorker>(
      context,
      periodicSyncConfiguration =
        PeriodicSyncConfiguration(repeat = RepeatInterval(interval = 15.minutes)),
    )
  }

  override suspend fun cancelPeriodicSync() {
    Sync.cancelPeriodicSync<AppFhirSyncWorker>(context)
  }
}
