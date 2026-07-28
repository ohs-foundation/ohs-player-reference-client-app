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

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/**
 * Drives the real [AuthService] against a [FakeSessionStore] + mocked [OidcAuthApi] (same pattern
 * as [AuthServiceTest], one layer up), so [AuthViewModel]'s state transitions are exercised through
 * real logic, not a hand-rolled fake of [AuthService] itself.
 */
class AuthViewModelTest {

  private class FakeSessionStore(initial: Session?) : SessionStore {
    private val _session = MutableStateFlow(initial)
    override val session: StateFlow<Session?> = _session.asStateFlow()

    override suspend fun load(): Session? = _session.value

    override suspend fun save(session: Session) {
      _session.value = session
    }

    override suspend fun clear() {
      _session.value = null
    }

    override suspend fun savePending(pending: PendingAuth) = Unit

    override suspend fun takePending(): PendingAuth? = null
  }

  private class FakeLauncher(
    override val redirectUri: String,
    private val onAuthorize: suspend (String) -> AuthResult,
  ) : AuthorizationLauncherApi {
    override suspend fun authorize(authUrl: String): AuthResult = onAuthorize(authUrl)
  }

  private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

  private fun apiThatNeverGetsCalled(): OidcAuthApi {
    val engine = MockEngine { respond("must not be called", HttpStatusCode.InternalServerError) }
    val client =
      HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
      }
    return OidcAuthApi(OAuthConfig("https://idp.example.org", "client", "openid"), client)
  }

  /**
   * [AuthService.login] always resolves discovery (to build the authorization URL) before
   * invoking the launcher, so a login-path test needs discovery to succeed — only the launcher's
   * own result determines the outcome under test.
   */
  private fun apiWithWorkingDiscovery(): OidcAuthApi {
    val discoveryBody =
      """
      {
        "authorization_endpoint": "https://idp.example.org/authorize",
        "token_endpoint": "https://idp.example.org/token",
        "userinfo_endpoint": "https://idp.example.org/userinfo",
        "end_session_endpoint": "https://idp.example.org/logout"
      }
      """
        .trimIndent()
    val engine = MockEngine { request ->
      if (request.url.encodedPath.endsWith("openid-configuration")) {
        respond(discoveryBody, HttpStatusCode.OK, jsonHeaders)
      } else {
        respond("must not be called", HttpStatusCode.InternalServerError)
      }
    }
    val client =
      HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
      }
    return OidcAuthApi(OAuthConfig("https://idp.example.org", "client", "openid"), client)
  }

  private fun session() =
    Session(
      accessToken = "a",
      refreshToken = "r",
      idToken = null,
      expiresInSeconds = 10_000_000_000L,
      obtainedAtEpochSeconds = 0,
      user = UserInfo(),
    )

  @Test
  fun bootstrap_withNoStoredSession_goesUnauthenticated() = runTest {
    val service =
      AuthService(
        OAuthConfig("https://idp.example.org", "c", "openid"),
        FakeSessionStore(null),
        apiThatNeverGetsCalled(),
      )
    val viewModel = AuthViewModel(service)
    val launcher = FakeLauncher("app://callback") { AuthResult.Canceled }

    viewModel.bootstrapForTest(launcher)

    assertEquals(AuthState.Unauthenticated, viewModel.state.value)
  }

  @Test
  fun bootstrap_withValidStoredSession_goesAuthenticatedWithoutContactingProvider() = runTest {
    val service =
      AuthService(
        OAuthConfig("https://idp.example.org", "c", "openid"),
        FakeSessionStore(session()),
        apiThatNeverGetsCalled(),
      )
    val viewModel = AuthViewModel(service)
    val launcher = FakeLauncher("app://callback") { AuthResult.Canceled }

    viewModel.bootstrapForTest(launcher)

    assertIs<AuthState.Authenticated>(viewModel.state.value)
  }

  @Test
  fun login_onError_setsErrorAndClearsSigningIn() = runTest {
    val service =
      AuthService(
        OAuthConfig("https://idp.example.org", "c", "openid"),
        FakeSessionStore(null),
        apiWithWorkingDiscovery(),
      )
    val viewModel = AuthViewModel(service)
    val launcher = FakeLauncher("app://callback") { AuthResult.Failure("network down") }

    viewModel.loginForTest(launcher)

    assertEquals("network down", viewModel.error.value)
    assertEquals(false, viewModel.signingIn.value)
  }

  @Test
  fun clearError_removesTheErrorMessage() = runTest {
    val service =
      AuthService(
        OAuthConfig("https://idp.example.org", "c", "openid"),
        FakeSessionStore(null),
        apiWithWorkingDiscovery(),
      )
    val viewModel = AuthViewModel(service)
    val launcher = FakeLauncher("app://callback") { AuthResult.Failure("network down") }
    viewModel.loginForTest(launcher)

    viewModel.clearError()

    assertNull(viewModel.error.value)
  }
}
