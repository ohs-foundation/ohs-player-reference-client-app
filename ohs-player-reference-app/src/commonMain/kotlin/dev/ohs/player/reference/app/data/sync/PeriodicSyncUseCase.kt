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

/**
 * Schedules this app's sync to run every 15 minutes while the device is online. Each platform
 * supplies its own implementation — see `WorkManagerPeriodicSyncUseCase` (Android),
 * `ForegroundPeriodicSyncUseCase` (JVM/web), and `IosPeriodicSyncUseCase` (iOS).
 */
interface PeriodicSyncUseCase {
  /**
   * Safe to call repeatedly — never schedules a duplicate/competing cycle. What "safe to call
   * repeatedly" means differs per platform: Android's `ExistingPeriodicWorkPolicy.KEEP` (default)
   * literally no-ops if already scheduled; iOS's scheduler cancels any pending request before
   * submitting a fresh one (a refresh, not a no-op); JVM/web's own implementation checks an
   * "already running" job and no-ops, for consistency with the other two.
   */
  suspend fun start()

  /** Cancels the periodic sync. No-op if none is scheduled. */
  suspend fun cancel()
}
