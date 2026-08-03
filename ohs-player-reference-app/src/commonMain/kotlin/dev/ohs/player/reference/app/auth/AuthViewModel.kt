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
package dev.ohs.player.reference.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ohs.player.reference.app.data.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Adapts [AuthService] to Compose. Deviates from opensrp: [service] is a constructor param
 * (Koin-injected via `viewModel { AuthViewModel(get()) }`) instead of self-constructed.
 * [AuthorizationLauncher] is platform UI, so it's passed in from the composable rather than
 * constructed here.
 */
internal class AuthViewModel(
  private val service: AuthService,
  private val syncManager: SyncManager,
) : ViewModel() {

  private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
  val state: StateFlow<AuthState> = _state.asStateFlow()

  private val _signingIn = MutableStateFlow(false)
  val signingIn: StateFlow<Boolean> = _signingIn.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  private var bootstrapped = false

  /** Restores any saved session and completes a web redirect login. Runs once. */
  fun bootstrap(launcher: AuthorizationLauncher) {
    if (bootstrapped) return
    bootstrapped = true
    viewModelScope.launch { runBootstrap(launcher) }
  }

  /**
   * Test seam: same bootstrap logic, but against [AuthorizationLauncherApi] so tests can fake it.
   */
  internal suspend fun bootstrapForTest(launcher: AuthorizationLauncherApi) = runBootstrap(launcher)

  private suspend fun runBootstrap(launcher: AuthorizationLauncherApi) {
    when (val redirect = service.completeRedirectLoginIfPresent(launcher)) {
      is LoginOutcome.Authenticated -> {
        _state.value = AuthState.Authenticated(redirect.session)
        return
      }
      is LoginOutcome.Error -> _error.value = redirect.message
      else -> Unit
    }
    val session = service.ensureFreshSession()
    if (session != null) {
      _state.value = AuthState.Authenticated(session)
      revalidateInBackground()
    } else {
      _state.value = AuthState.Unauthenticated
    }
  }

  private fun revalidateInBackground() {
    viewModelScope.launch { if (!service.revalidateSession()) signOut() }
  }

  fun login(launcher: AuthorizationLauncher) {
    viewModelScope.launch { runLogin(launcher) }
  }

  /** Test seam: same login logic, but against [AuthorizationLauncherApi] so tests can fake it. */
  internal suspend fun loginForTest(launcher: AuthorizationLauncherApi) = runLogin(launcher)

  private suspend fun runLogin(launcher: AuthorizationLauncherApi) {
    _signingIn.value = true
    _error.value = null
    when (val outcome = service.login(launcher)) {
      is LoginOutcome.Authenticated -> _state.value = AuthState.Authenticated(outcome.session)
      is LoginOutcome.Redirecting -> Unit // web: page is unloading to the provider
      is LoginOutcome.Canceled -> Unit
      is LoginOutcome.Error -> _error.value = outcome.message
    }
    _signingIn.value = false
  }

  fun clearError() {
    _error.value = null
  }

  fun logout() {
    viewModelScope.launch { runLogout() }
  }

  /** Test seam: same logout logic, runnable directly without `viewModelScope.launch`. */
  internal suspend fun logoutForTest() = runLogout()

  private suspend fun runLogout() {
    service.logout()
    signOut()
  }

  /**
   * Ends the local session: stops any in-flight and recurring sync before dropping to the login
   * screen, so neither can keep firing with a stale/absent token after a logout or a server-side
   * revoke. Local FHIR data is intentionally left on disk — logout clears the session, not the
   * data.
   */
  private suspend fun signOut() {
    syncManager.cancelSyncNow()
    syncManager.cancelPeriodicSync()
    _state.value = AuthState.Unauthenticated
  }
}
