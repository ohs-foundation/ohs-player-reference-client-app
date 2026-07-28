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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Subset of the OIDC discovery document ({issuer}/.well-known/openid-configuration) we use. Lets
 * the app stay provider-agnostic — endpoints are read from here rather than hardcoded per provider.
 */
@Serializable
internal data class OpenIdConfiguration(
  @SerialName("authorization_endpoint") val authorizationEndpoint: String,
  @SerialName("token_endpoint") val tokenEndpoint: String,
  @SerialName("userinfo_endpoint") val userInfoEndpoint: String = "",
  @SerialName("end_session_endpoint") val endSessionEndpoint: String = "",
)

/** Raw token endpoint response from the OIDC provider. */
@Serializable
internal data class TokenResponse(
  @SerialName("access_token") val accessToken: String,
  @SerialName("refresh_token") val refreshToken: String? = null,
  @SerialName("id_token") val idToken: String? = null,
  @SerialName("token_type") val tokenType: String = "Bearer",
  @SerialName("expires_in") val expiresIn: Long = 0,
  @SerialName("refresh_expires_in") val refreshExpiresIn: Long = 0,
  @SerialName("scope") val scope: String? = null,
)

/** Subset of the provider's `userinfo` response we care about. */
@Serializable
data class UserInfo(
  @SerialName("sub") val subject: String = "",
  @SerialName("preferred_username") val username: String = "",
  @SerialName("name") val fullName: String = "",
  @SerialName("email") val email: String = "",
)

/**
 * The persisted session — tokens plus user identity. Stored encrypted in KSafe.
 * [obtainedAtEpochSeconds] + [expiresInSeconds] let us know when to refresh.
 */
@Serializable
data class Session(
  val accessToken: String,
  val refreshToken: String?,
  val idToken: String?,
  val expiresInSeconds: Long,
  val obtainedAtEpochSeconds: Long,
  val user: UserInfo,
) {
  /** True when the access token has expired (with a small safety skew). */
  fun isAccessTokenExpired(nowEpochSeconds: Long, skewSeconds: Long = 30): Boolean =
    nowEpochSeconds >= (obtainedAtEpochSeconds + expiresInSeconds - skewSeconds)
}

/**
 * Short-lived state we must remember across the authorization round-trip (especially on web, where
 * the page reloads). Stored encrypted in KSafe.
 */
@Serializable internal data class PendingAuth(val codeVerifier: String, val state: String)

/** Result of an [AuthorizationLauncher.authorize] call. */
sealed interface AuthResult {
  /** Web only: the browser is navigating away; the result arrives on reload. */
  data object Redirecting : AuthResult

  /** The full callback URL (contains `code` + `state`, or `error`). */
  data class Success(val callbackUrl: String) : AuthResult

  /** The user dismissed the browser/auth sheet. */
  data object Canceled : AuthResult

  data class Failure(val message: String) : AuthResult
}

/** High-level outcome of a login attempt, surfaced to the UI. */
sealed interface LoginOutcome {
  data class Authenticated(val session: Session) : LoginOutcome

  /** Web: page is unloading to the provider; nothing more to do this load. */
  data object Redirecting : LoginOutcome

  data object Canceled : LoginOutcome

  data class Error(val message: String) : LoginOutcome
}

/**
 * Server-side liveness of a session. [Unknown] means the provider could not be reached (offline);
 * callers MUST treat it as "keep the session" so connectivity loss never logs a field user out.
 */
enum class SessionStatus {
  Active,
  Revoked,
  Unknown,
}

/** Auth state the UI renders against. */
sealed interface AuthState {
  data object Loading : AuthState

  data object Unauthenticated : AuthState

  data class Authenticated(val session: Session) : AuthState
}
