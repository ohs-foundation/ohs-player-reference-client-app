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
import androidx.compose.runtime.remember
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.browser.window

internal actual fun createKSafe(): KSafe = KSafe()

// secureRandomBytes lives in jsMain / wasmJsMain — Web Crypto interop differs
// between the two web targets, so it can't be shared from webMain.

/**
 * Web login uses a full-page redirect to the identity provider (the recommended SPA flow).
 * [authorize] navigates away and the page unloads, so it returns [AuthResult.Redirecting]; the
 * [PendingAuth] saved in KSafe survives the round-trip. On the next load [consumeRedirectCallback]
 * picks up the result.
 */
actual class AuthorizationLauncher(actual override val redirectUri: String) :
  AuthorizationLauncherApi {

  actual override suspend fun authorize(authUrl: String): AuthResult {
    window.location.href = authUrl
    return AuthResult.Redirecting
  }

  actual override fun consumeRedirectCallback(): String? {
    val search = window.location.search
    if (!search.contains("code=") && !search.contains("error=")) return null
    val href = window.location.href
    // Strip the query so a refresh doesn't replay the (single-use) code.
    window.history.replaceState(null, "", window.location.pathname)
    return href
  }
}

@Composable
actual fun rememberAuthorizationLauncher(): AuthorizationLauncher = remember {
  AuthorizationLauncher(GeneratedAuthConfig.WEB_REDIRECT_URL)
}
