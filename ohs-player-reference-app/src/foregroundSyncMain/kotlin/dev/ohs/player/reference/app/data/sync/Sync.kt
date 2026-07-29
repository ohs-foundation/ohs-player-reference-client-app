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

/**
 * Foreground-only one-time sync scheduler shared by Desktop (JVM) and web (js, wasmJs) — neither
 * platform has a native OS background scheduler (no WorkManager, no BGTaskScheduler), so sync only
 * runs while the host process (JVM process / browser tab) stays alive. Mirrors
 * kotlin-fhir-engine's engine-app `Sync` object, trimmed to the one-time-sync surface this app
 * uses (no periodic sync).
 */
internal object Sync {
  private val scope = CoroutineScope(SupervisorJob() + syncDispatcher)
  private val mutex = Mutex()
  private val activeSyncs = mutableMapOf<String, SyncHandle>()
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
        val maxRetries = retryConfiguration?.maxRetries ?: 0
        var attempt = 0
        var lastResult: SyncJobStatus = SyncJobStatus.Failed()

        while (attempt <= maxRetries) {
          if (attempt > 0) {
            delay(computeBackoffDelayMillis(retryConfiguration!!, attempt - 1).milliseconds)
          }
          statusFlow.emit(CurrentSyncJobStatus.Running(SyncJobStatus.Started()))
          lastResult =
            try {
              runSyncWithTimeout(taskFactory(), uniqueWorkName, syncTimeout) { syncJobStatus ->
                statusFlow.emit(CurrentSyncJobStatus.Running(syncJobStatus))
              }
            } catch (e: CancellationException) {
              throw e
            } catch (e: Exception) {
              Logger.e(e) { "One-time sync failed: ${e.message}" }
              SyncJobStatus.Failed()
            }
          if (lastResult is SyncJobStatus.Succeeded) break
          attempt++
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
