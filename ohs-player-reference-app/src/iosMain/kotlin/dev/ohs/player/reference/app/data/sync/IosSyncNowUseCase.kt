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

import co.touchlab.kermit.Logger
import dev.ohs.fhir.engine.FhirEngineProvider
import dev.ohs.fhir.engine.sync.SyncJobStatus
import dev.ohs.fhir.engine.sync.runSync
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

/**
 * Runs a one-time sync on a dedicated [Dispatchers.IO]-backed scope decoupled from the caller, so
 * backgrounding the triggering screen doesn't cancel an in-flight network sync. Mirrors
 * kotlin-fhir-engine's engine-app `FhirSyncController.ios.kt`: iOS suspends networking for
 * backgrounded apps with no active background task, so an in-progress sync job is cancelled on
 * [UIApplicationDidEnterBackgroundNotification] and relaunched from scratch on
 * [UIApplicationWillEnterForegroundNotification], with [invoke] suspending across the relaunch
 * until a terminal result arrives.
 */
class IosSyncNowUseCase : SyncNowUseCase {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private var currentJob: Job? = null
  private var currentStatusFlow: MutableSharedFlow<SyncJobStatus>? = null
  private var syncWasRunning = false

  init {
    NSNotificationCenter.defaultCenter.addObserverForName(
      UIApplicationDidEnterBackgroundNotification,
      null,
      null,
    ) { _ ->
      if (currentJob?.isActive == true) {
        syncWasRunning = true
        currentJob?.cancel()
        Logger.d { "IosSyncNowUseCase: sync suspended on background" }
      }
    }

    NSNotificationCenter.defaultCenter.addObserverForName(
      UIApplicationWillEnterForegroundNotification,
      null,
      null,
    ) { _ ->
      if (syncWasRunning) {
        syncWasRunning = false
        launchSyncJob()
        Logger.d { "IosSyncNowUseCase: sync restarted on foreground" }
      }
    }
  }

  override suspend fun invoke(): SyncJobStatus {
    val statusFlow = MutableSharedFlow<SyncJobStatus>(replay = 1)
    currentStatusFlow = statusFlow
    launchSyncJob()
    return statusFlow.first { it is SyncJobStatus.Succeeded || it is SyncJobStatus.Failed }
  }

  private fun launchSyncJob() {
    val statusFlow = currentStatusFlow ?: return
    currentJob?.cancel()
    currentJob =
      scope.launch {
        val result =
          try {
            AppFhirSyncTask(FhirEngineProvider.getInstance()).runSync(taskName = null) {}
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            Logger.e(e) { "IosSyncNowUseCase: one-time sync failed" }
            SyncJobStatus.Failed()
          }
        statusFlow.emit(result)
      }
  }
}
