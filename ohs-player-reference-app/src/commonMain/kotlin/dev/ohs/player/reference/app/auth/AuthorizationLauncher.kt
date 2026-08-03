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

import androidx.compose.runtime.Composable

/**
 * Opens the system browser to the identity provider's authorization page and returns the redirect
 * callback. Each platform uses its idiomatic mechanism:
 * - Android: Chrome Custom Tabs + a custom-scheme deep link.
 * - iOS: ASWebAuthenticationSession (returns the callback URL directly).
 * - Desktop: a localhost loopback HTTP server + the system browser.
 * - Web: a full-page redirect; the callback is read on the next page load via
 *   [consumeRedirectCallback].
 *
 * [redirectUri] is the platform-correct value and must be registered at the identity provider. It
 * is used in BOTH the authorize request and the token exchange, so the launcher owns it.
 */
/** The subset of [AuthorizationLauncher] that [AuthService] depends on — lets tests fake it. */
internal interface AuthorizationLauncherApi {
  val redirectUri: String

  suspend fun authorize(authUrl: String): AuthResult

  /**
   * Web only: if this page load is a redirect back from the provider, returns the callback URL (and
   * clears it from the address bar). Returns null otherwise and on every non-web platform.
   */
  fun consumeRedirectCallback(): String?
}

expect class AuthorizationLauncher : AuthorizationLauncherApi {
  override val redirectUri: String

  /**
   * Launches the auth flow. Suspends until the callback is received ([AuthResult.Success]) on
   * platforms that can await it; returns [AuthResult.Redirecting] on web, where the page unloads.
   */
  override suspend fun authorize(authUrl: String): AuthResult

  override fun consumeRedirectCallback(): String?
}

/** Obtains a launcher, wiring in any platform context (e.g. the Android Activity). */
@Composable expect fun rememberAuthorizationLauncher(): AuthorizationLauncher
