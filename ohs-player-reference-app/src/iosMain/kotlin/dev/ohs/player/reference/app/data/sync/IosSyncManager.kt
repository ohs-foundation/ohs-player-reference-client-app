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
import dev.ohs.player.reference.app.data.DataChangeSignal
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

/** Must match `BGTaskSchedulerPermittedIdentifiers` in `iosApp/iosApp/Info.plist`. */
internal const val PERIODIC_SYNC_TASK_IDENTIFIER = "dev.ohs.player.reference.app.sync.periodic"

/**
 * iOS [SyncManager]. Periodic sync runs as a `BGProcessingTask` via [IosBgSyncScheduler];
 * registration happens in the constructor (not lazily) because `BGTaskScheduler` requires it before
 * `applicationDidFinishLaunching` returns — see [IosBgSyncScheduler]'s docs and
 * [dev.ohs.player.reference.app.MainViewController], which constructs this eagerly.
 *
 * One-time sync runs on a dedicated [Dispatchers.IO]-backed scope decoupled from the caller, so
 * backgrounding the triggering screen doesn't cancel an in-flight network sync. Mirrors
 * kotlin-fhir-engine's engine-app `FhirSyncController.ios.kt`: iOS suspends networking for
 * backgrounded apps with no active background task, so an in-progress one-time sync is cancelled on
 * [UIApplicationDidEnterBackgroundNotification] and relaunched from scratch on
 * [UIApplicationWillEnterForegroundNotification], with [syncNow] suspending across the relaunch
 * until a terminal result arrives.
 */
class IosSyncManager : SyncManager {
  private val scheduler =
    IosBgSyncScheduler(
      taskIdentifier = PERIODIC_SYNC_TASK_IDENTIFIER,
      taskFactory = { AppFhirSyncTask(FhirEngineProvider.getInstance()) },
    )

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private var currentJob: Job? = null
  private var currentStatusFlow: MutableSharedFlow<SyncJobStatus>? = null
  private var syncWasRunning = false

  init {
    scheduler.register()

    NSNotificationCenter.defaultCenter.addObserverForName(
      UIApplicationDidEnterBackgroundNotification,
      null,
      null,
    ) { _ ->
      if (currentJob?.isActive == true) {
        syncWasRunning = true
        currentJob?.cancel()
        Logger.d { "IosSyncManager: sync suspended on background" }
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
        Logger.d { "IosSyncManager: sync restarted on foreground" }
      }
    }
  }

  override suspend fun syncNow(): SyncJobStatus {
    val statusFlow = MutableSharedFlow<SyncJobStatus>(replay = 1)
    currentStatusFlow = statusFlow
    launchSyncJob()
    return statusFlow.first { it is SyncJobStatus.Succeeded || it is SyncJobStatus.Failed }
  }

  override suspend fun cancelSyncNow() {
    syncWasRunning = false
    currentJob?.cancel()
    currentStatusFlow?.emit(SyncJobStatus.Failed())
  }

  override suspend fun startPeriodicSync() {
    scheduler.schedule()
  }

  override suspend fun cancelPeriodicSync() {
    scheduler.cancel()
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
            Logger.e(e) { "IosSyncManager: one-time sync failed" }
            SyncJobStatus.Failed()
          }
        if (result is SyncJobStatus.Succeeded) DataChangeSignal.notifyChanged()
        statusFlow.emit(result)
      }
  }
}
