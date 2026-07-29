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
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.player.reference.app.data.repository.InMemorySampleFhirRepository
import dev.ohs.player.reference.app.data.sync.SyncNowUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class InitialSyncViewModelTest {

  private class FakeSyncNowUseCase(private val result: suspend () -> SyncJobStatus) : SyncNowUseCase {
    var invocationCount = 0
      private set

    override suspend fun invoke(): SyncJobStatus {
      invocationCount++
      return result()
    }

    override suspend fun cancel() {}
  }

  private val json = Json { ignoreUnknownKeys = true }

  private fun testPatient() =
    json.decodeFromString(Patient.serializer(), """{"resourceType": "Patient", "id": "p1"}""")

  @Test
  fun start_withExistingData_goesPassedWithoutSyncing() = runTest {
    val repository = InMemorySampleFhirRepository()
    repository.upsert(testPatient())
    val syncNowUseCase = FakeSyncNowUseCase { SyncJobStatus.Succeeded() }
    val viewModel = InitialSyncViewModel(repository, syncNowUseCase)

    viewModel.start().join()

    assertEquals(InitialSyncGateState.Passed, viewModel.state.value)
    assertEquals(0, syncNowUseCase.invocationCount)
  }

  @Test
  fun start_withNoData_syncsThenPasses() = runTest {
    val repository = InMemorySampleFhirRepository()
    val syncNowUseCase = FakeSyncNowUseCase { SyncJobStatus.Succeeded() }
    val viewModel = InitialSyncViewModel(repository, syncNowUseCase)

    viewModel.start().join()

    assertEquals(InitialSyncGateState.Passed, viewModel.state.value)
    assertEquals(1, syncNowUseCase.invocationCount)
  }

  @Test
  fun start_withNoDataAndSyncFails_goesFailed() = runTest {
    val repository = InMemorySampleFhirRepository()
    val syncNowUseCase = FakeSyncNowUseCase { SyncJobStatus.Failed() }
    val viewModel = InitialSyncViewModel(repository, syncNowUseCase)

    viewModel.start().join()

    assertIs<InitialSyncGateState.Failed>(viewModel.state.value)
  }

  @Test
  fun retry_afterFailure_syncsAgain() = runTest {
    val repository = InMemorySampleFhirRepository()
    var shouldFail = true
    val syncNowUseCase =
      FakeSyncNowUseCase { if (shouldFail) SyncJobStatus.Failed() else SyncJobStatus.Succeeded() }
    val viewModel = InitialSyncViewModel(repository, syncNowUseCase)
    viewModel.start().join()
    assertIs<InitialSyncGateState.Failed>(viewModel.state.value)

    shouldFail = false
    viewModel.retry().join()

    assertEquals(InitialSyncGateState.Passed, viewModel.state.value)
    assertEquals(2, syncNowUseCase.invocationCount)
  }

  @Test
  fun continueAnyway_movesToPassed() = runTest {
    val repository = InMemorySampleFhirRepository()
    val syncNowUseCase = FakeSyncNowUseCase { SyncJobStatus.Failed() }
    val viewModel = InitialSyncViewModel(repository, syncNowUseCase)
    viewModel.start().join()
    assertIs<InitialSyncGateState.Failed>(viewModel.state.value)

    viewModel.continueAnyway()

    assertEquals(InitialSyncGateState.Passed, viewModel.state.value)
  }
}
