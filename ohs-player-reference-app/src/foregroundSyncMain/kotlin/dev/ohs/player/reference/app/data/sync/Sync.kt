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
import dev.ohs.fhir.engine.sync.BackoffPolicy
import dev.ohs.fhir.engine.sync.CurrentSyncJobStatus
import dev.ohs.fhir.engine.sync.FhirDataStore
import dev.ohs.fhir.engine.sync.FhirSyncTask
import dev.ohs.fhir.engine.sync.RetryConfiguration
import dev.ohs.fhir.engine.sync.SyncJobStatus
import dev.ohs.fhir.engine.sync.defaultRetryConfiguration
import dev.ohs.fhir.engine.sync.runSync
import dev.ohs.fhir.engine.sync.syncDispatcher
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Supplies each platform's best-effort "is the device online" check for [Sync.periodicSync]. */
internal expect fun isNetworkConnected(): Boolean

/**
 * Foreground-only sync scheduler shared by Desktop (JVM) and web (js, wasmJs) — neither platform
 * has a native OS background scheduler (no WorkManager, no BGTaskScheduler), so sync only runs
 * while the host process (JVM process / browser tab) stays alive. Mirrors kotlin-fhir-engine's
 * engine-app `Sync` object.
 */
internal object Sync {
  private val scope = CoroutineScope(SupervisorJob() + syncDispatcher)
  private val mutex = Mutex()
  private val activeSyncs = mutableMapOf<String, SyncHandle>()
  private val activePeriodicJobs = mutableMapOf<String, Job>()
  private val fhirDataStore: FhirDataStore by lazy { FhirEngineProvider.getFhirDataStore() }

  /**
   * Executes a one-time sync using [FhirSyncTask] instances created by [taskFactory].
   *
   * If a one-time sync for [T] is already active, the existing [Flow] is returned immediately
   * without starting a new job.
   *
   * @param taskFactory Creates a fresh [FhirSyncTask] for each attempt (including retries).
   * @param retryConfiguration Retry policy on failure, or null to disable retries.
   * @param syncTimeout Maximum duration for a single sync attempt. If exceeded, the attempt is
   *   treated as failed and subject to retry. `null` means no timeout.
   * @return A [Flow] of [CurrentSyncJobStatus] tracking the full sync lifecycle.
   */
  suspend inline fun <reified T : FhirSyncTask> oneTimeSync(
    noinline taskFactory: () -> T,
    retryConfiguration: RetryConfiguration? = defaultRetryConfiguration,
    syncTimeout: Duration? = null,
  ): Flow<CurrentSyncJobStatus> {
    val uniqueWorkName = "${T::class.simpleName}-oneTimeSync"
    return runOneTimeSync(uniqueWorkName, taskFactory, retryConfiguration, syncTimeout)
  }

  /** Cancels an active one-time sync for [T]. No-op if none is active. */
  suspend inline fun <reified T : FhirSyncTask> cancelOneTimeSync() {
    cancelSync("${T::class.simpleName}-oneTimeSync")
  }

  /**
   * Schedules a recurring foreground sync using [FhirSyncTask] instances created by [taskFactory].
   * Safe to call repeatedly — a cycle already running for [T] is left alone, never duplicated.
   * Skips (not fails/retries) any cycle where [isNetworkConnected] returns false.
   *
   * @param repeatInterval Delay between the end of one cycle and the start of the next.
   * @param retryConfiguration Retry policy applied within each cycle, or null to disable retries.
   */
  suspend inline fun <reified T : FhirSyncTask> periodicSync(
    noinline taskFactory: () -> T,
    repeatInterval: Duration = 15.minutes,
    retryConfiguration: RetryConfiguration? = defaultRetryConfiguration,
  ) {
    runPeriodicSync(
      "${T::class.simpleName}-periodicSync",
      taskFactory,
      repeatInterval,
      retryConfiguration,
    )
  }

  /** Cancels the recurring periodic sync for [T]. No-op if none is active. */
  suspend inline fun <reified T : FhirSyncTask> cancelPeriodicSync() {
    val job = mutex.withLock { activePeriodicJobs.remove("${T::class.simpleName}-periodicSync") }
    job?.cancel()
  }

  suspend fun runOneTimeSync(
    uniqueWorkName: String,
    taskFactory: () -> FhirSyncTask,
    retryConfiguration: RetryConfiguration?,
    syncTimeout: Duration? = null,
  ): Flow<CurrentSyncJobStatus> {
    mutex
      .withLock { activeSyncs[uniqueWorkName] }
      ?.takeIf { it.job.isActive }
      ?.let {
        return it.progressChannel
      }

    val statusFlow = MutableSharedFlow<CurrentSyncJobStatus>(replay = 1)
    storeUniqueWorkNameInDataStore(fhirDataStore, uniqueWorkName)

    statusFlow.emit(CurrentSyncJobStatus.Enqueued)

    val job =
      scope.launch {
        val lastResult =
          runAttemptsWithRetry(taskFactory, uniqueWorkName, retryConfiguration, syncTimeout) {
            statusFlow.emit(it)
          }
        when (lastResult) {
          is SyncJobStatus.Succeeded ->
            statusFlow.emit(CurrentSyncJobStatus.Succeeded(lastResult.timestamp))
          else ->
            statusFlow.emit(
              CurrentSyncJobStatus.Failed(
                (lastResult as? SyncJobStatus.Failed)?.timestamp ?: Clock.System.now()
              )
            )
        }
        removeUniqueWorkNameInDataStore(fhirDataStore, uniqueWorkName)
        mutex.withLock { activeSyncs.remove(uniqueWorkName) }
      }

    mutex.withLock { activeSyncs[uniqueWorkName] = SyncHandle(job, statusFlow) }
    return statusFlow
  }

  suspend fun runPeriodicSync(
    uniqueWorkName: String,
    taskFactory: () -> FhirSyncTask,
    repeatInterval: Duration,
    retryConfiguration: RetryConfiguration?,
  ) {
    mutex.withLock { activePeriodicJobs[uniqueWorkName] }?.takeIf { it.isActive }?.let { return }

    val job =
      scope.launch {
        while (true) {
          if (isNetworkConnected()) {
            runAttemptsWithRetry(taskFactory, uniqueWorkName, retryConfiguration, syncTimeout = null) {}
          } else {
            Logger.d { "Periodic sync cycle skipped for $uniqueWorkName — offline" }
          }
          delay(repeatInterval)
        }
      }
    mutex.withLock { activePeriodicJobs[uniqueWorkName] = job }
  }

  suspend fun cancelSync(uniqueWorkName: String) {
    val handle = mutex.withLock { activeSyncs[uniqueWorkName] }
    if (handle == null || !handle.job.isActive) {
      Logger.w { "No active sync found for: $uniqueWorkName" }
      return
    }
    handle.progressChannel.emit(CurrentSyncJobStatus.Cancelled)
    handle.job.cancel()
    mutex.withLock { activeSyncs.remove(uniqueWorkName) }
    removeUniqueWorkNameInDataStore(fhirDataStore, uniqueWorkName)
  }

  /** Runs [taskFactory]'s sync with retry, reporting each intermediate status via [onStatus]. */
  private suspend fun runAttemptsWithRetry(
    taskFactory: () -> FhirSyncTask,
    uniqueWorkName: String,
    retryConfiguration: RetryConfiguration?,
    syncTimeout: Duration?,
    onStatus: suspend (CurrentSyncJobStatus) -> Unit,
  ): SyncJobStatus {
    val maxRetries = retryConfiguration?.maxRetries ?: 0
    var attempt = 0
    var lastResult: SyncJobStatus = SyncJobStatus.Failed()

    while (attempt <= maxRetries) {
      if (attempt > 0) {
        delay(computeBackoffDelayMillis(retryConfiguration!!, attempt - 1).milliseconds)
      }
      onStatus(CurrentSyncJobStatus.Running(SyncJobStatus.Started()))
      lastResult =
        try {
          runSyncWithTimeout(taskFactory(), uniqueWorkName, syncTimeout) { syncJobStatus ->
            onStatus(CurrentSyncJobStatus.Running(syncJobStatus))
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Logger.e(e) { "Sync failed: ${e.message}" }
          SyncJobStatus.Failed()
        }
      if (lastResult is SyncJobStatus.Succeeded) break
      attempt++
    }
    return lastResult
  }

  private suspend fun runSyncWithTimeout(
    task: FhirSyncTask,
    taskName: String?,
    syncTimeout: Duration?,
    onProgress: suspend (SyncJobStatus) -> Unit,
  ): SyncJobStatus {
    val call = suspend { task.runSync(taskName = taskName, onProgress = onProgress) }
    return if (syncTimeout != null) {
      withTimeoutOrNull(syncTimeout) { call() }
        ?: run {
          Logger.w { "Sync timed out after $syncTimeout" }
          SyncJobStatus.Failed()
        }
    } else {
      call()
    }
  }

  private suspend fun storeUniqueWorkNameInDataStore(
    fhirDataStore: FhirDataStore,
    uniqueWorkName: String,
  ) {
    if (fhirDataStore.fetchUniqueWorkName(uniqueWorkName) == null) {
      fhirDataStore.storeUniqueWorkName(key = uniqueWorkName, value = uniqueWorkName)
    }
  }

  private suspend fun removeUniqueWorkNameInDataStore(
    fhirDataStore: FhirDataStore,
    uniqueWorkName: String,
  ) {
    if (fhirDataStore.fetchUniqueWorkName(uniqueWorkName) != null) {
      fhirDataStore.removeUniqueWorkName(key = uniqueWorkName)
    }
  }

  private fun computeBackoffDelayMillis(config: RetryConfiguration, attempt: Int): Long {
    val baseDelayMs = config.backoffCriteria.backoffDelay.inWholeMilliseconds
    return when (config.backoffCriteria.backoffPolicy) {
      BackoffPolicy.EXPONENTIAL -> baseDelayMs * (1L shl attempt)
      BackoffPolicy.LINEAR -> baseDelayMs
    }
  }
}

private data class SyncHandle(
  val job: Job,
  val progressChannel: MutableSharedFlow<CurrentSyncJobStatus>,
)
