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
package dev.ohs.player.reference.app.feature.home

import dev.ohs.fhir.engine.sync.FhirDataStore
import dev.ohs.fhir.engine.sync.SyncJobStatus
import dev.ohs.fhir.engine.sync.createDataStore
import dev.ohs.player.reference.app.data.sync.SyncManager
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest

private class RecordingSyncManager(
  private val fhirDataStore: FhirDataStore,
  private val result: suspend () -> SyncJobStatus,
) : SyncManager {
  var invocationCount = 0
    private set

  var cancelCount = 0
    private set

  override suspend fun syncNow(): SyncJobStatus {
    invocationCount++
    val status = result()
    // Mirrors FhirSynchronizer's real behavior of persisting the timestamp on every terminal
    // outcome, so the ViewModel's re-read-after-sync behavior is exercised realistically.
    fhirDataStore.writeLastSyncTimestamp(status.timestamp)
    return status
  }

  override suspend fun cancelSyncNow() {
    cancelCount++
  }

  override suspend fun startPeriodicSync() {}

  override suspend fun cancelPeriodicSync() {}
}

class HomeViewModelTest {

  private fun newFhirDataStore(): FhirDataStore {
    val path = Files.createTempFile("home-viewmodel-test", ".preferences_pb").toString()
    return FhirDataStore(createDataStore { path })
  }

  @Test
  fun initialState_withNoPriorSync_hasNoLastSyncedAt() = runTest {
    val viewModel =
      HomeViewModel(
        RecordingSyncManager(newFhirDataStore()) { SyncJobStatus.Succeeded() },
        newFhirDataStore(),
      )

    assertNull(viewModel.uiState.value.lastSyncedAt)
  }

  @Test
  fun syncNow_onSuccess_clearsIsSyncingAndPopulatesLastSyncedAt() = runTest {
    val fhirDataStore = newFhirDataStore()
    val viewModel =
      HomeViewModel(
        RecordingSyncManager(fhirDataStore) { SyncJobStatus.Succeeded() },
        fhirDataStore,
      )

    viewModel.syncNow()?.join()

    val state = viewModel.uiState.value
    assertEquals(false, state.isSyncing)
    assertNull(state.syncError)
    assertNotNull(state.lastSyncedAt)
  }

  @Test
  fun syncNow_onFailure_clearsIsSyncingAndSetsSyncError() = runTest {
    val fhirDataStore = newFhirDataStore()
    val viewModel =
      HomeViewModel(RecordingSyncManager(fhirDataStore) { SyncJobStatus.Failed() }, fhirDataStore)

    viewModel.syncNow()?.join()

    val state = viewModel.uiState.value
    assertEquals(false, state.isSyncing)
    assertEquals(SyncError.Failed, state.syncError)
  }

  @Test
  fun syncNow_whenUseCaseThrows_setsSyncError() = runTest {
    val fhirDataStore = newFhirDataStore()
    val viewModel =
      HomeViewModel(
        RecordingSyncManager(fhirDataStore) { throw RuntimeException("network down") },
        fhirDataStore,
      )

    viewModel.syncNow()?.join()

    val state = viewModel.uiState.value
    assertEquals(false, state.isSyncing)
    assertEquals(SyncError.Failed, state.syncError)
  }

  @Test
  fun syncNow_whileAlreadySyncing_doesNotStartASecondSync() = runTest {
    val fhirDataStore = newFhirDataStore()
    val fake = RecordingSyncManager(fhirDataStore) { SyncJobStatus.Succeeded() }
    val viewModel = HomeViewModel(fake, fhirDataStore)

    val first = viewModel.syncNow()
    val second = viewModel.syncNow()
    first?.join()

    assertNull(second)
    assertEquals(1, fake.invocationCount)
  }

  @Test
  fun cancelSync_whileSyncing_callsUseCaseCancelAndSetsCancelledMessage() = runTest {
    val fhirDataStore = newFhirDataStore()
    val releaseSyncResult = CompletableDeferred<SyncJobStatus>()
    val fake = RecordingSyncManager(fhirDataStore) { releaseSyncResult.await() }
    val viewModel = HomeViewModel(fake, fhirDataStore)

    val job = viewModel.syncNow()
    viewModel.cancelSync()
    releaseSyncResult.complete(SyncJobStatus.Failed())
    job?.join()

    assertEquals(1, fake.cancelCount)
    val state = viewModel.uiState.value
    assertEquals(false, state.isSyncing)
    assertEquals(SyncError.Cancelled, state.syncError)
  }

  @Test
  fun cancelSync_whileNotSyncing_isNoOp() = runTest {
    val fhirDataStore = newFhirDataStore()
    val fake = RecordingSyncManager(fhirDataStore) { SyncJobStatus.Succeeded() }
    val viewModel = HomeViewModel(fake, fhirDataStore)

    viewModel.cancelSync()

    assertEquals(0, fake.cancelCount)
  }

  @Test
  fun clearSyncError_removesTheErrorMessage() = runTest {
    val fhirDataStore = newFhirDataStore()
    val viewModel =
      HomeViewModel(RecordingSyncManager(fhirDataStore) { SyncJobStatus.Failed() }, fhirDataStore)
    viewModel.syncNow()?.join()

    viewModel.clearSyncError()

    assertNull(viewModel.uiState.value.syncError)
  }
}
