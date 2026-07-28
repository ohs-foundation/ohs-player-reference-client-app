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
import dev.ohs.fhir.engine.FhirEngine
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
  private val fhirEngine: FhirEngine,
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
    viewModelScope.launch {
      when (val redirect = service.completeRedirectLoginIfPresent(launcher)) {
        is LoginOutcome.Authenticated -> {
          _state.value = AuthState.Authenticated(redirect.session)
          return@launch
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
  }

  /**
   * Test seam: same bootstrap logic, but against [AuthorizationLauncherApi] so tests can fake it.
   */
  internal suspend fun bootstrapForTest(launcher: AuthorizationLauncherApi) {
    val session = service.ensureFreshSession()
    _state.value =
      if (session != null) AuthState.Authenticated(session) else AuthState.Unauthenticated
  }

  private fun revalidateInBackground() {
    viewModelScope.launch {
      if (!service.revalidateSession()) _state.value = AuthState.Unauthenticated
    }
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
    fhirEngine.clearDatabase()
    _state.value = AuthState.Unauthenticated
  }
}
