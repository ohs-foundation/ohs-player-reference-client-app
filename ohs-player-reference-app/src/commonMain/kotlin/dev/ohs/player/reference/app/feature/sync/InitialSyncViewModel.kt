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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ohs.fhir.engine.sync.SyncJobStatus
import dev.ohs.player.reference.app.data.repository.FhirRepository
import dev.ohs.player.reference.app.data.sync.PeriodicSyncUseCase
import dev.ohs.player.reference.app.data.sync.SyncNowUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** State the blocking initial-sync gate renders against. */
sealed interface InitialSyncGateState {
  data object Checking : InitialSyncGateState

  data object Syncing : InitialSyncGateState

  data class Failed(val message: String) : InitialSyncGateState

  data object Passed : InitialSyncGateState
}

/**
 * Gates a freshly-authenticated session behind a one-time blocking sync when the local FHIR
 * database is empty. [start] deliberately has no "already ran" guard — it must fully re-run its
 * check every time it's called, because Koin can hand back the same ViewModel instance across a
 * logout → login round-trip (the top-level `App()` composables aren't scoped per auth session), and
 * a fresh login must always be re-evaluated (e.g. after
 * [dev.ohs.player.reference.app.auth.AuthViewModel.logout] wipes local data).
 */
class InitialSyncViewModel(
  private val fhirRepository: FhirRepository,
  private val syncNowUseCase: SyncNowUseCase,
  private val periodicSyncUseCase: PeriodicSyncUseCase,
) : ViewModel() {

  private val _state = MutableStateFlow<InitialSyncGateState>(InitialSyncGateState.Checking)
  val state: StateFlow<InitialSyncGateState> = _state.asStateFlow()

  fun start(): Job = viewModelScope.launch { runCheck() }

  fun retry(): Job = viewModelScope.launch { runSync() }

  fun continueAnyway(): Job = viewModelScope.launch { passGate() }

  private suspend fun runCheck() {
    _state.value = InitialSyncGateState.Checking
    if (fhirRepository.hasAnyData()) {
      passGate()
    } else {
      runSync()
    }
  }

  private suspend fun runSync() {
    _state.value = InitialSyncGateState.Syncing
    val result =
      try {
        syncNowUseCase.invoke()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        SyncJobStatus.Failed()
      }
    when (result) {
      is SyncJobStatus.Succeeded -> passGate()
      else ->
        _state.value =
          InitialSyncGateState.Failed("Sync failed. Please check your connection and try again.")
    }
  }

  private suspend fun passGate() {
    periodicSyncUseCase.start()
    _state.value = InitialSyncGateState.Passed
  }
}
