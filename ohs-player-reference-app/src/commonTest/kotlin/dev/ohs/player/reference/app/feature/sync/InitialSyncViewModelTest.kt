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
package dev.ohs.player.reference.app.feature.sync

import dev.ohs.fhir.engine.sync.SyncJobStatus
import dev.ohs.player.reference.app.data.sync.FakeSyncManager
import dev.ohs.player.reference.app.data.sync.InitialSyncStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private class FakeInitialSyncStore(private var complete: Boolean = false) : InitialSyncStore {
  override suspend fun isComplete(): Boolean = complete

  override suspend fun markComplete() {
    complete = true
  }
}

class InitialSyncViewModelTest {

  @Test
  fun start_whenAlreadyComplete_passesWithoutSyncing() = runTest {
    val syncManager = FakeSyncManager()
    val viewModel = InitialSyncViewModel(syncManager, FakeInitialSyncStore(complete = true))

    viewModel.start().join()

    assertEquals(InitialSyncGateState.Passed, viewModel.state.value)
    assertEquals(0, syncManager.syncNowCount)
    assertEquals(1, syncManager.startPeriodicCount)
  }

  @Test
  fun start_whenNotComplete_syncsThenPassesAndMarksComplete() = runTest {
    val syncManager = FakeSyncManager()
    val store = FakeInitialSyncStore()
    val viewModel = InitialSyncViewModel(syncManager, store)

    viewModel.start().join()

    assertEquals(InitialSyncGateState.Passed, viewModel.state.value)
    assertEquals(1, syncManager.syncNowCount)
    assertEquals(1, syncManager.startPeriodicCount)
    assertTrue(store.isComplete())
  }

  @Test
  fun start_whenSyncFails_goesFailedAndStaysIncomplete() = runTest {
    val syncManager = FakeSyncManager { SyncJobStatus.Failed() }
    val store = FakeInitialSyncStore()
    val viewModel = InitialSyncViewModel(syncManager, store)

    viewModel.start().join()

    assertIs<InitialSyncGateState.Failed>(viewModel.state.value)
    assertEquals(0, syncManager.startPeriodicCount)
    assertFalse(store.isComplete())
  }

  @Test
  fun retry_afterFailure_syncsAgainAndMarksComplete() = runTest {
    var shouldFail = true
    val syncManager = FakeSyncManager {
      if (shouldFail) SyncJobStatus.Failed() else SyncJobStatus.Succeeded()
    }
    val store = FakeInitialSyncStore()
    val viewModel = InitialSyncViewModel(syncManager, store)
    viewModel.start().join()
    assertIs<InitialSyncGateState.Failed>(viewModel.state.value)

    shouldFail = false
    viewModel.retry().join()

    assertEquals(InitialSyncGateState.Passed, viewModel.state.value)
    assertEquals(2, syncManager.syncNowCount)
    assertEquals(1, syncManager.startPeriodicCount)
    assertTrue(store.isComplete())
  }

  @Test
  fun continueAnyway_passesButDoesNotMarkComplete() = runTest {
    val syncManager = FakeSyncManager { SyncJobStatus.Failed() }
    val store = FakeInitialSyncStore()
    val viewModel = InitialSyncViewModel(syncManager, store)
    viewModel.start().join()
    assertIs<InitialSyncGateState.Failed>(viewModel.state.value)

    viewModel.continueAnyway().join()

    assertEquals(InitialSyncGateState.Passed, viewModel.state.value)
    assertEquals(1, syncManager.startPeriodicCount)
    assertFalse(store.isComplete())
  }
}
