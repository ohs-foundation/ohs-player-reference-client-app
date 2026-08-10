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

import dev.ohs.fhir.engine.FhirEngine
import dev.ohs.fhir.engine.FhirEngineConfiguration
import dev.ohs.fhir.engine.FhirEngineProvider
import dev.ohs.fhir.engine.sync.AcceptLocalConflictResolver
import dev.ohs.fhir.engine.sync.ConflictResolver
import dev.ohs.fhir.engine.sync.CurrentSyncJobStatus
import dev.ohs.fhir.engine.sync.DownloadWorkManager
import dev.ohs.fhir.engine.sync.FhirSyncTask
import dev.ohs.fhir.engine.sync.download.DownloadRequest
import dev.ohs.fhir.engine.sync.upload.HttpCreateMethod
import dev.ohs.fhir.engine.sync.upload.HttpUpdateMethod
import dev.ohs.fhir.engine.sync.upload.UploadStrategy
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.fhir.model.r4.terminologies.ResourceType
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * A no-op [FhirSyncTask] whose download step optionally blocks on [gate] before completing empty
 * (no requests, no local changes) — real enough for [dev.ohs.fhir.engine.sync.runSync] to reach
 * [dev.ohs.fhir.engine.sync.SyncJobStatus.Succeeded] without ever touching the network. Matches the
 * shape kotlin-fhir-engine's own `DownloaderImplTest.TestDownloadWorkManager` uses.
 */
private class TestFhirSyncTask(private val gate: CompletableDeferred<Unit>? = null) : FhirSyncTask {
  override fun getFhirEngine(): FhirEngine = FhirEngineProvider.getInstance()

  override fun getDownloadWorkManager(): DownloadWorkManager =
    object : DownloadWorkManager {
      private var requested = false

      override suspend fun getNextRequest(): DownloadRequest? {
        if (requested) return null
        requested = true
        gate?.await()
        return null
      }

      override suspend fun getSummaryRequestUrls(): Map<ResourceType, String> = emptyMap()

      override suspend fun processResponse(response: Resource): Collection<Resource> = emptyList()
    }

  override fun getConflictResolver(): ConflictResolver = AcceptLocalConflictResolver

  override fun getUploadStrategy(): UploadStrategy =
    UploadStrategy.forBundleRequest(
      methodForCreate = HttpCreateMethod.PUT,
      methodForUpdate = HttpUpdateMethod.PATCH,
      squash = true,
      bundleSize = 500,
    )
}

class SyncTest {

  @BeforeTest
  fun setUp() = runTest {
    // Matches the established pattern in FhirEngineRepositoryTest: FhirEngineProvider is a
    // process-wide singleton that throws if initialized twice, so guard it and reset state
    // between tests instead of re-initializing.
    if (FhirEngineProvider.isNotInitialized()) {
      FhirEngineProvider.init(
        FhirEngineConfiguration(
          storageDirectory = Files.createTempDirectory("sync-test").toString()
        )
      )
    }
    FhirEngineProvider.getInstance().clearDatabase()
  }

  @Test
  fun cancelOneTimeSync_whileRunning_emitsCancelled() = runTest {
    val gate = CompletableDeferred<Unit>()
    val statusFlow = Sync.oneTimeSync(taskFactory = { TestFhirSyncTask(gate) })

    // Let the sync actually start (past Enqueued) before cancelling.
    statusFlow.first { it is CurrentSyncJobStatus.Running }

    Sync.cancelOneTimeSync<TestFhirSyncTask>()

    val terminal = statusFlow.first { it !is CurrentSyncJobStatus.Running }
    assertIs<CurrentSyncJobStatus.Cancelled>(terminal)
  }

  @Test
  fun cancelOneTimeSync_withNoActiveSync_doesNotThrow() = runTest {
    Sync.cancelOneTimeSync<TestFhirSyncTask>()
    assertTrue(true) // reaching here means no-op didn't throw
  }

  @Test
  fun periodicSync_start_runsFirstCycleImmediately() = runTest {
    val firstAttempt = CompletableDeferred<Unit>()

    Sync.periodicSync(
      taskFactory = {
        firstAttempt.complete(Unit)
        TestFhirSyncTask()
      },
      // Long enough that only one cycle should fire during this test's lifetime.
      repeatInterval = 1.minutes,
    )

    // No withTimeout wrapper here on purpose: withTimeout schedules its cancellation on the
    // *test* dispatcher's virtual-time scheduler, which auto-advances past it near-instantly
    // once this coroutine looks "idle" (runTest doesn't know about the real work happening on
    // Sync's separate syncDispatcher) — a virtual/real time mismatch that made this test flake.
    // A plain await() is a real suspension resolved only when the background coroutine actually
    // completes it; runTest's own real-time dispatch timeout is the safety net if it never does.
    firstAttempt.await()

    Sync.cancelPeriodicSync<TestFhirSyncTask>()
  }

  @Test
  fun periodicSync_calledTwice_doesNotStartASecondCompetingLoop() = runTest {
    var startCount = 0
    val firstAttemptStarted = CompletableDeferred<Unit>()
    val factory: () -> TestFhirSyncTask = {
      startCount++
      firstAttemptStarted.complete(Unit)
      TestFhirSyncTask()
    }

    Sync.periodicSync(taskFactory = factory, repeatInterval = 1.minutes)
    firstAttemptStarted.await()
    // The first cycle's loop is now sleeping the 1-minute interval, so it's still "active" —
    // this second call must see that and no-op rather than starting a competing loop.
    Sync.periodicSync(taskFactory = factory, repeatInterval = 1.minutes)

    assertEquals(1, startCount)

    Sync.cancelPeriodicSync<TestFhirSyncTask>()
  }
}
