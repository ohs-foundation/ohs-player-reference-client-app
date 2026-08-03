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

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Drives the Authorization-Code-with-PKCE flow against the configured OIDC provider and keeps the
 * [SessionStore] up to date. UI-agnostic; [AuthViewModel] adapts it to Compose state.
 *
 * Unlike opensrp's version, this constructor has NO default parameter values — Koin always supplies
 * [config], [repository], and [api] (see `authModule` in `AppModule.kt`), so self-constructing
 * defaults would be dead code here.
 */
internal class AuthService(
  private val config: OAuthConfig,
  private val repository: SessionStore,
  private val api: OidcAuthApi,
) {

  /** Starts an interactive login. */
  suspend fun login(launcher: AuthorizationLauncherApi): LoginOutcome {
    val pkce = Pkce.generate()
    val state = Pkce.randomState()
    repository.savePending(PendingAuth(codeVerifier = pkce.verifier, state = state))

    val authUrl =
      runCatching { buildAuthorizationUrl(launcher.redirectUri, pkce, state) }
        .getOrElse {
          return LoginOutcome.Error(it.message ?: "Could not reach the sign-in provider")
        }
    return when (val result = launcher.authorize(authUrl)) {
      is AuthResult.Redirecting -> LoginOutcome.Redirecting
      is AuthResult.Success -> completeLogin(result.callbackUrl, launcher.redirectUri)
      is AuthResult.Canceled -> LoginOutcome.Canceled
      is AuthResult.Failure -> LoginOutcome.Error(result.message)
    }
  }

  /**
   * Web: on app start, finishes a login if this page load is a redirect back from the provider.
   * Returns null when there is nothing to complete.
   */
  suspend fun completeRedirectLoginIfPresent(launcher: AuthorizationLauncherApi): LoginOutcome? {
    val callbackUrl = launcher.consumeRedirectCallback() ?: return null
    return completeLogin(callbackUrl, launcher.redirectUri)
  }

  /** Exchanges the callback's `code` for tokens and persists the session. */
  private suspend fun completeLogin(callbackUrl: String, redirectUri: String): LoginOutcome {
    val params = Url(callbackUrl).parameters
    params["error"]?.let { error ->
      return LoginOutcome.Error(params["error_description"] ?: error)
    }
    val code = params["code"] ?: return LoginOutcome.Error("Missing authorization code")
    val returnedState = params["state"]

    val pending = repository.takePending()
    if (pending == null || pending.state != returnedState) {
      return LoginOutcome.Error("Invalid state — possible CSRF, please try again")
    }

    return runCatching {
        val tokens = api.exchangeCode(code, pending.codeVerifier, redirectUri)
        val session = tokens.toSession(api.fetchUserInfo(tokens.accessToken))
        repository.save(session)
        LoginOutcome.Authenticated(session)
      }
      .getOrElse { LoginOutcome.Error(it.message ?: "Sign-in failed") }
  }

  /**
   * Returns a usable session at startup, refreshing an expired access token when possible.
   *
   * Offline-safe: the session is cleared (→ login) ONLY on a definitive provider rejection
   * ([AuthException]). A transient/offline failure keeps the stored session. Server-side revocation
   * while the access token is still locally valid is caught separately (and non-blockingly) by
   * [revalidateSession].
   */
  suspend fun ensureFreshSession(): Session? {
    val session = repository.load() ?: return null
    if (!session.isAccessTokenExpired(now())) return session

    val refreshed = tryRefresh(session)
    return refreshed ?: repository.session.value
  }

  /**
   * Confirms a locally-valid session is still alive server-side via the userinfo probe. Clears it
   * only on a definitive revoke; offline is a no-op. Meant to run in the background after startup
   * so it never blocks an offline launch.
   */
  suspend fun revalidateSession(): Boolean {
    val session = repository.session.value ?: repository.load() ?: return false
    return when (api.sessionStatus(session.accessToken)) {
      SessionStatus.Revoked -> {
        repository.clear()
        false
      }
      SessionStatus.Active,
      SessionStatus.Unknown -> true
    }
  }

  /** Refresh hook for [FhirBearerAuthenticator]'s eventual retry path, and internal use. */
  internal suspend fun refreshTokensForRequest(): Session? {
    val session = repository.session.value ?: repository.load() ?: return null
    return tryRefresh(session)
  }

  private suspend fun tryRefresh(session: Session): Session? {
    val refreshToken =
      session.refreshToken
        ?: run {
          repository.clear()
          return null
        }
    return try {
      val tokens = api.refresh(refreshToken)
      tokens.toSession(session.user).also { repository.save(it) }
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (rejected: AuthException) {
      repository.clear()
      null
    } catch (offline: Throwable) {
      null
    }
  }

  suspend fun logout() {
    repository.session.value?.refreshToken?.let { api.logout(it) }
    repository.clear()
  }

  private suspend fun buildAuthorizationUrl(
    redirectUri: String,
    pkce: PkcePair,
    state: String,
  ): String =
    URLBuilder(api.endpoints().authorizationEndpoint)
      .apply {
        parameters.append("client_id", config.clientId)
        parameters.append("response_type", "code")
        parameters.append("scope", config.scopes)
        parameters.append("redirect_uri", redirectUri)
        parameters.append("code_challenge", pkce.challenge)
        parameters.append("code_challenge_method", pkce.method)
        parameters.append("state", state)
      }
      .buildString()

  private fun TokenResponse.toSession(user: UserInfo): Session =
    Session(
      accessToken = accessToken,
      refreshToken = refreshToken,
      idToken = idToken,
      expiresInSeconds = expiresIn,
      obtainedAtEpochSeconds = now(),
      user = user,
    )

  @OptIn(ExperimentalTime::class)
  private fun now(): Long = Clock.System.now().toEpochMilliseconds() / 1000
}
