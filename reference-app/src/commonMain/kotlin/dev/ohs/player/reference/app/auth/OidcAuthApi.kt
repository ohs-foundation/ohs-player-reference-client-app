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
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Provider-agnostic OpenID Connect client (discovery, token exchange, refresh, userinfo, logout).
 * Endpoints are resolved from the provider's discovery document rather than hardcoded, so any
 * standards-compliant OIDC provider works by changing only [OAuthConfig.issuer]. The Ktor engine is
 * selected automatically from each platform's classpath (OkHttp / CIO / Darwin / JS).
 */
internal class OidcAuthApi(
  private val config: OAuthConfig,
  private val httpClient: HttpClient = defaultHttpClient(),
) {

  private val endpointsMutex = Mutex()
  private var cachedEndpoints: OidcEndpoints? = null

  /** Resolves and caches the provider endpoints via OIDC discovery. */
  suspend fun endpoints(): OidcEndpoints =
    cachedEndpoints
      ?: endpointsMutex.withLock { cachedEndpoints ?: discover().also { cachedEndpoints = it } }

  private suspend fun discover(): OidcEndpoints {
    val response = httpClient.get(config.discoveryUrl)
    check(response.status.isSuccess()) {
      "Could not reach the sign-in provider (HTTP ${response.status.value} from ${config.discoveryUrl})"
    }
    val doc: OpenIdConfiguration = response.body()
    return OidcEndpoints(
      authorizationEndpoint = doc.authorizationEndpoint,
      tokenEndpoint = doc.tokenEndpoint,
      userInfoEndpoint = doc.userInfoEndpoint,
      endSessionEndpoint = doc.endSessionEndpoint,
    )
  }

  /** Exchanges an authorization `code` (+ PKCE verifier) for tokens. */
  suspend fun exchangeCode(code: String, codeVerifier: String, redirectUri: String): TokenResponse =
    httpClient
      .submitForm(
        url = endpoints().tokenEndpoint,
        formParameters =
          parameters {
            append("grant_type", "authorization_code")
            append("client_id", config.clientId)
            append("code", code)
            append("redirect_uri", redirectUri)
            append("code_verifier", codeVerifier)
          },
      )
      .requireTokens()

  /** Refreshes an expired access token using the refresh token. */
  suspend fun refresh(refreshToken: String): TokenResponse =
    httpClient
      .submitForm(
        url = endpoints().tokenEndpoint,
        formParameters =
          parameters {
            append("grant_type", "refresh_token")
            append("client_id", config.clientId)
            append("refresh_token", refreshToken)
          },
      )
      .requireTokens()

  private suspend fun HttpResponse.requireTokens(): TokenResponse {
    if (!status.isSuccess()) {
      val raw = runCatching { bodyAsText() }.getOrNull().orEmpty()
      throw AuthException(oauthErrorMessage(raw, status.value))
    }
    return body()
  }

  suspend fun fetchUserInfo(accessToken: String): UserInfo =
    httpClient
      .get(endpoints().userInfoEndpoint) {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
      }
      .body()

  /**
   * Liveness check via the userinfo endpoint. Fails OPEN: unreachable provider / timeout /
   * discovery failure returns [SessionStatus.Unknown] so the caller keeps the local session
   * (offline must never force a logout).
   */
  suspend fun sessionStatus(accessToken: String): SessionStatus =
    try {
      val response =
        httpClient.get(endpoints().userInfoEndpoint) {
          header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
      when {
        response.status.isSuccess() -> SessionStatus.Active
        response.status == HttpStatusCode.Unauthorized -> SessionStatus.Revoked
        else -> SessionStatus.Unknown
      }
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (unreachable: Throwable) {
      SessionStatus.Unknown
    }

  /** Best-effort server-side logout; failures are ignored (local clear still wins). */
  suspend fun logout(refreshToken: String) {
    runCatching {
      val endSession = endpoints().endSessionEndpoint
      if (endSession.isBlank()) return@runCatching
      val response =
        httpClient.submitForm(
          url = endSession,
          formParameters =
            parameters {
              append("client_id", config.clientId)
              append("refresh_token", refreshToken)
            },
        )
      if (response.status != HttpStatusCode.NoContent && !response.status.isSuccess()) {
        response.bodyAsText()
      }
    }
  }

  companion object {
    fun defaultHttpClient(): HttpClient = HttpClient {
      install(ContentNegotiation) {
        json(
          Json {
            ignoreUnknownKeys = true
            isLenient = true
          }
        )
      }
      install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
      }
    }
  }
}

private fun HttpStatusCode.isSuccess(): Boolean = value in 200..299

/** A failed auth/token request, carrying a human-readable reason for the UI. */
internal class AuthException(message: String) : Exception(message)

private fun oauthErrorMessage(body: String, statusCode: Int): String {
  val parsed =
    runCatching {
        val obj = Json.parseToJsonElement(body).jsonObject
        val error = obj["error"]?.jsonPrimitive?.contentOrNull
        val description = obj["error_description"]?.jsonPrimitive?.contentOrNull
        listOfNotNull(error, description).joinToString(": ")
      }
      .getOrNull()
  return parsed?.takeIf { it.isNotBlank() } ?: "Authentication request failed (HTTP $statusCode)"
}
