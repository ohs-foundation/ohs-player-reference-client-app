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

/** Must match `BGTaskSchedulerPermittedIdentifiers` in `iosApp/iosApp/Info.plist`. */
internal const val PERIODIC_SYNC_TASK_IDENTIFIER = "dev.ohs.player.reference.app.sync.periodic"

/**
 * Schedules this app's sync as a `BGProcessingTask` via [IosBgSyncScheduler]. Registration happens
 * in the constructor (not lazily) — see [IosBgSyncScheduler]'s docs on why that matters, and
 * [dev.ohs.player.reference.app.MainViewController], which constructs this eagerly.
 */
class IosPeriodicSyncUseCase : PeriodicSyncUseCase {
  private val scheduler =
    IosBgSyncScheduler(
      taskIdentifier = PERIODIC_SYNC_TASK_IDENTIFIER,
      taskFactory = { AppFhirSyncTask(FhirEngineProvider.getInstance()) },
    )

  init {
    scheduler.register()
  }

  override suspend fun start() {
    scheduler.schedule()
  }

  override suspend fun cancel() {
    scheduler.cancel()
  }
}
