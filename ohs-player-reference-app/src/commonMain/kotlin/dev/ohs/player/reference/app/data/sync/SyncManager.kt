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

/**
 * This app's single sync seam. Scheduling is inherently platform-specific — WorkManager on Android,
 * `BGTaskScheduler` on iOS, an in-process foreground loop on JVM/web — so each platform supplies
 * its own implementation (`WorkManagerSyncManager`, `ForegroundSyncManager`, `IosSyncManager`).
 * Routing every sync operation through one interface also keeps the ViewModels unit-testable
 * against a fake: the engine's `runSync` is a top-level extension over a global singleton, so there
 * is no seam to fake without it.
 */
interface SyncManager {
  /** Runs a one-time sync and returns its terminal result. */
  suspend fun syncNow(): SyncJobStatus

  /** Cancels an in-flight [syncNow]. No-op if no one-time sync is running. */
  suspend fun cancelSyncNow()

  /**
   * Schedules the recurring background sync (every 15 minutes while online). Safe to call
   * repeatedly — never schedules a duplicate/competing cycle. What that means differs per platform:
   * Android's `ExistingPeriodicWorkPolicy.KEEP` no-ops if already scheduled; iOS cancels any
   * pending request before submitting a fresh one; JVM/web checks an already-running job and
   * no-ops.
   */
  suspend fun startPeriodicSync()

  /** Cancels the periodic sync. No-op if none is scheduled. */
  suspend fun cancelPeriodicSync()
}
