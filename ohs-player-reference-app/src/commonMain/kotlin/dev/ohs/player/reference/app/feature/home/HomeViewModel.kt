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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ohs.fhir.sync.FhirDataStore
import dev.ohs.fhir.sync.SyncJobStatus
import dev.ohs.player.reference.app.data.sync.SyncNowUseCase
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class HomeUiState(
  val isSyncing: Boolean = false,
  val lastSyncedAt: String? = null,
  val syncError: String? = null,
)

class HomeViewModel(
  private val syncNowUseCase: SyncNowUseCase,
  private val fhirDataStore: FhirDataStore,
) : ViewModel() {
  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      val lastSyncedAt = fhirDataStore.readLastSyncTimestamp()?.toDisplayString()
      _uiState.update { it.copy(lastSyncedAt = lastSyncedAt) }
    }
  }

  /**
   * Triggers a one-time sync. Returns `null` without starting a new sync if one is already in
   * progress; otherwise returns the launched [Job] (primarily so tests can `join()` it).
   */
  fun syncNow(): Job? {
    if (_uiState.value.isSyncing) return null
    _uiState.update { it.copy(isSyncing = true, syncError = null) }
    return viewModelScope.launch {
      val result =
        try {
          syncNowUseCase.invoke()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          SyncJobStatus.Failed()
        }
      when (result) {
        is SyncJobStatus.Succeeded -> {
          val lastSyncedAt = fhirDataStore.readLastSyncTimestamp()?.toDisplayString()
          _uiState.update { it.copy(isSyncing = false, lastSyncedAt = lastSyncedAt) }
        }
        else ->
          _uiState.update {
            it.copy(isSyncing = false, syncError = "Sync failed. Please try again.")
          }
      }
    }
  }

  fun clearSyncError() {
    _uiState.update { it.copy(syncError = null) }
  }
}

private fun Instant.toDisplayString(): String {
  val local = toLocalDateTime(TimeZone.currentSystemDefault())
  return "${local.date} ${local.hour.toString().padStart(2, '0')}:" +
    local.minute.toString().padStart(2, '0')
}
