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

import dev.ohs.fhir.engine.FhirEngineProvider
import dev.ohs.fhir.engine.sync.CurrentSyncJobStatus
import dev.ohs.fhir.engine.sync.SyncJobStatus
import kotlinx.coroutines.flow.first

/**
 * Runs a one-time sync through the shared foreground [Sync] scheduler — Desktop and web only sync
 * while their host process is alive (see [Sync]'s docs for why).
 */
class ForegroundSyncNowUseCase : SyncNowUseCase {
  override suspend fun invoke(): SyncJobStatus {
    val terminalStatus =
      Sync.oneTimeSync(taskFactory = { AppFhirSyncTask(FhirEngineProvider.getInstance()) }).first {
        it is CurrentSyncJobStatus.Succeeded || it is CurrentSyncJobStatus.Failed
      }
    return when (terminalStatus) {
      is CurrentSyncJobStatus.Succeeded -> SyncJobStatus.Succeeded()
      else -> SyncJobStatus.Failed()
    }
  }
}
