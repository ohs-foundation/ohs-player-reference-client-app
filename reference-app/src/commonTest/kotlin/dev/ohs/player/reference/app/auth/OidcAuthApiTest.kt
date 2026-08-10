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
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class OidcAuthApiTest {

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

  private fun apiWith(
    onToken: MockRequestHandleScope.() -> HttpResponseData = {
      respond(
        """{"access_token":"a","refresh_token":"r","expires_in":300,"token_type":"Bearer"}""",
        HttpStatusCode.OK,
        jsonHeaders,
      )
    },
    onUserInfo: MockRequestHandleScope.() -> HttpResponseData = {
      respond("{}", HttpStatusCode.OK, jsonHeaders)
    },
  ): OidcAuthApi {
    val engine = MockEngine { request ->
      val path = request.url.encodedPath
      when {
        path.endsWith("openid-configuration") ->
          respond(discoveryBody, HttpStatusCode.OK, jsonHeaders)
        path.endsWith("/token") -> onToken()
        path.endsWith("/userinfo") -> onUserInfo()
        else -> respond("not found", HttpStatusCode.NotFound)
      }
    }
    val client =
      HttpClient(engine) {
        install(ContentNegotiation) {
          json(
            Json {
              ignoreUnknownKeys = true
              isLenient = true
            }
          )
        }
      }
    return OidcAuthApi(OAuthConfig("https://idp.example.org", "client", "openid"), client)
  }

  @Test
  fun exchangeCode_returnsTokensFromMockedEndpoint() = runTest {
    val tokens = apiWith().exchangeCode("code123", "verifier", "app://callback")
    assertEquals("a", tokens.accessToken)
    assertEquals("r", tokens.refreshToken)
  }

  @Test
  fun exchangeCode_onProviderError_throwsAuthExceptionWithMessage() = runTest {
    val api =
      apiWith(
        onToken = {
          respond(
            """{"error":"invalid_grant","error_description":"Token is not active"}""",
            HttpStatusCode.BadRequest,
            jsonHeaders,
          )
        }
      )
    val error = assertFailsWith<AuthException> { api.exchangeCode("bad", "v", "app://callback") }
    assertEquals("invalid_grant: Token is not active", error.message)
  }

  @Test
  fun sessionStatus_mapsHttpResultsToSessionStatus() = runTest {
    assertEquals(
      SessionStatus.Active,
      apiWith(onUserInfo = { respond("{}", HttpStatusCode.OK, jsonHeaders) }).sessionStatus("t"),
    )
    assertEquals(
      SessionStatus.Revoked,
      apiWith(onUserInfo = { respond("", HttpStatusCode.Unauthorized) }).sessionStatus("t"),
    )
    assertEquals(
      SessionStatus.Unknown,
      apiWith(onUserInfo = { respond("", HttpStatusCode.InternalServerError) }).sessionStatus("t"),
    )
    assertEquals(
      SessionStatus.Unknown,
      apiWith(onUserInfo = { throw RuntimeException("offline") }).sessionStatus("t"),
    )
  }

  @Test
  fun fetchUserInfo_parsesUserFields() = runTest {
    val api =
      apiWith(
        onUserInfo = {
          respond(
            """{"sub":"u1","preferred_username":"jdoe","name":"Jane Doe","email":"j@example.org"}""",
            HttpStatusCode.OK,
            jsonHeaders,
          )
        }
      )
    val user = api.fetchUserInfo("token")
    assertEquals("jdoe", user.username)
    assertEquals("j@example.org", user.email)
  }
}
