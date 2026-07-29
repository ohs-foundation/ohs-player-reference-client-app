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
import dev.ohs.fhir.engine.sync.PeriodicSyncConfiguration
import dev.ohs.fhir.engine.sync.RepeatInterval
import dev.ohs.fhir.engine.sync.Sync
import kotlin.time.Duration.Companion.minutes

/**
 * Schedules this app's sync as periodic WorkManager work, reusing [AppFhirSyncWorker] (the same
 * Worker class [WorkManagerSyncNowUseCase] uses for one-time sync). [PeriodicSyncConfiguration]'s
 * default `SyncConstraints` already requires `NetworkType.CONNECTED`, so WorkManager itself defers
 * runs until the device is online — no separate connectivity check needed here.
 */
class WorkManagerPeriodicSyncUseCase(private val context: Context) : PeriodicSyncUseCase {
  override suspend fun start() {
    Sync.periodicSync<AppFhirSyncWorker>(
      context,
      periodicSyncConfiguration =
        PeriodicSyncConfiguration(repeat = RepeatInterval(interval = 15.minutes)),
    )
  }

  override suspend fun cancel() {
    Sync.cancelPeriodicSync<AppFhirSyncWorker>(context)
  }
}
