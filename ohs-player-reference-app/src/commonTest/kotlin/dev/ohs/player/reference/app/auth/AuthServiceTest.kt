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
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/**
 * Verifies the offline-safe session logic: a logout happens ONLY on a definitive provider
 * rejection (4xx), never on a network/offline failure, which must keep the local session.
 */
class AuthServiceTest {

  private class FakeSessionStore(initial: Session?) : SessionStore {
    private val _session = MutableStateFlow(initial)
    override val session: StateFlow<Session?> = _session.asStateFlow()
    var cleared = false
      private set
    private var pending: PendingAuth? = null

    override suspend fun load(): Session? = _session.value

    override suspend fun save(session: Session) {
      _session.value = session
    }

    override suspend fun clear() {
      _session.value = null
      cleared = true
    }

    override suspend fun savePending(pending: PendingAuth) {
      this.pending = pending
    }

    override suspend fun takePending(): PendingAuth? = pending.also { pending = null }
  }

  private class FakeLauncher(
    override val redirectUri: String,
    private val onAuthorize: suspend (String) -> AuthResult,
  ) : AuthorizationLauncherApi {
    override suspend fun authorize(authUrl: String): AuthResult = onAuthorize(authUrl)
  }

  private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
  private val discoveryBody =
    """
    {
      "authorization_endpoint": "https://idp.example.org/authorize",
      "token_endpoint": "https://idp.example.org/token",
      "userinfo_endpoint": "https://idp.example.org/userinfo",
      "end_session_endpoint": "https://idp.example.org/logout"
    }
    """
      .trimIndent()
  private val tokenBody =
    """{"access_token":"new-access","refresh_token":"new-refresh","expires_in":300,"token_type":"Bearer"}"""
  private val invalidGrantBody = """{"error":"invalid_grant","error_description":"Token is not active"}"""

  private fun session(expired: Boolean, refreshToken: String? = "refresh-token") =
    Session(
      accessToken = "old-access",
      refreshToken = refreshToken,
      idToken = null,
      expiresInSeconds = if (expired) 0L else 10_000_000_000L,
      obtainedAtEpochSeconds = 0L,
      user = UserInfo(),
    )

  private fun apiWith(
    onToken: MockRequestHandleScope.() -> HttpResponseData = {
      respond(tokenBody, HttpStatusCode.OK, jsonHeaders)
    },
    onUserInfo: MockRequestHandleScope.() -> HttpResponseData = {
      respond("{}", HttpStatusCode.OK, jsonHeaders)
    },
  ): OidcAuthApi {
    val engine = MockEngine { request ->
      val path = request.url.encodedPath
      when {
        path.endsWith("openid-configuration") -> respond(discoveryBody, HttpStatusCode.OK, jsonHeaders)
        path.endsWith("/token") -> onToken()
        path.endsWith("/userinfo") -> onUserInfo()
        else -> respond("not found", HttpStatusCode.NotFound)
      }
    }
    val client =
      HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
      }
    return OidcAuthApi(OAuthConfig("https://idp.example.org", "client", "openid"), client)
  }

  private fun service(store: SessionStore, api: OidcAuthApi) =
    AuthService(OAuthConfig("https://idp.example.org", "client", "openid"), store, api)

  // ---- ensureFreshSession ----------------------------------------------------

  @Test
  fun refreshSuccess_updatesSession() = runTest {
    val store = FakeSessionStore(session(expired = true))
    val result = service(store, apiWith()).ensureFreshSession()

    assertNotNull(result)
    assertEquals("new-access", result.accessToken)
    assertEquals("new-access", store.session.value?.accessToken)
    assertFalse(store.cleared)
  }

  @Test
  fun refreshRejectedByProvider_logsOut() = runTest {
    val store = FakeSessionStore(session(expired = true))
    val api = apiWith(onToken = { respond(invalidGrantBody, HttpStatusCode.BadRequest, jsonHeaders) })

    val result = service(store, api).ensureFreshSession()

    assertNull(result)
    assertTrue(store.cleared, "a definitive provider rejection must log the user out")
    assertNull(store.session.value)
  }

  @Test
  fun refreshOffline_keepsSession() = runTest {
    val original = session(expired = true)
    val store = FakeSessionStore(original)
    val api = apiWith(onToken = { throw RuntimeException("offline") })

    val result = service(store, api).ensureFreshSession()

    assertEquals(original, result, "a network failure must NOT log the user out")
    assertFalse(store.cleared)
  }

  @Test
  fun validToken_returnsWithoutContactingProvider() = runTest {
    val original = session(expired = false)
    val store = FakeSessionStore(original)
    val api =
      apiWith(
        onToken = { throw RuntimeException("must not be called") },
        onUserInfo = { throw RuntimeException("must not be called") },
      )

    assertEquals(original, service(store, api).ensureFreshSession())
    assertFalse(store.cleared)
  }

  // ---- revalidateSession ------------------------------------------------------

  @Test
  fun revalidateRevoked_logsOut() = runTest {
    val store = FakeSessionStore(session(expired = false))
    val api = apiWith(onUserInfo = { respond("", HttpStatusCode.Unauthorized) })

    assertFalse(service(store, api).revalidateSession())
    assertTrue(store.cleared)
  }

  @Test
  fun revalidateOffline_keepsSession() = runTest {
    val store = FakeSessionStore(session(expired = false))
    val api = apiWith(onUserInfo = { throw RuntimeException("offline") })

    assertTrue(service(store, api).revalidateSession(), "offline probe must not log out")
    assertFalse(store.cleared)
  }

  // ---- login / buildAuthorizationUrl -----------------------------------------

  @Test
  fun login_buildsAuthorizationUrlWithRequiredPkceParamsAndCompletesOnMatchingState() = runTest {
    val store = FakeSessionStore(null)
    var capturedAuthUrl: String? = null
    val fakeLauncher =
      FakeLauncher(
        redirectUri = "app://callback",
        onAuthorize = { authUrl ->
          capturedAuthUrl = authUrl
          val state = Url(authUrl).parameters["state"].orEmpty()
          AuthResult.Success("app://callback?code=abc123&state=$state")
        },
      )

    val outcome = service(store, apiWith()).login(fakeLauncher)

    assertTrue(outcome is LoginOutcome.Authenticated)
    val builtUrl = capturedAuthUrl
    assertNotNull(builtUrl)
    val params = Url(builtUrl).parameters
    assertEquals("code", params["response_type"])
    assertEquals("S256", params["code_challenge_method"])
    assertEquals("app://callback", params["redirect_uri"])
    assertNotNull(params["state"])
    assertNotNull(params["code_challenge"])
  }

  @Test
  fun login_withMismatchedState_returnsError() = runTest {
    val store = FakeSessionStore(null)
    val fakeLauncher =
      FakeLauncher(
        redirectUri = "app://callback",
        onAuthorize = { AuthResult.Success("app://callback?code=abc123&state=wrong-state") },
      )

    val outcome = service(store, apiWith()).login(fakeLauncher)

    assertTrue(outcome is LoginOutcome.Error)
  }
}
