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

/**
 * Loads the persisted session and refreshes an expired access token, so a headless/background sync
 * (Android WorkManager, iOS `BGProcessingTask`) hands [FhirBearerAuthenticator] a valid Bearer
 * token even when the UI bootstrap that normally hydrates [SessionRepository] never ran.
 *
 * Builds its own [AuthService] over the baked [OAuthConfig.Default] and the singleton
 * [SessionRepository] rather than resolving one from Koin: iOS background launches don't call
 * `initKoin`, so nothing is registered there. Offline-safe — a transient/offline failure keeps the
 * stored session (see [AuthService.ensureFreshSession]); it only returns null when there is no
 * usable session at all, in which case the sync will simply 401 and be retried later.
 */
internal suspend fun ensureFreshSessionForSync() {
  headlessAuthService.ensureFreshSession()
}

/**
 * One instance per process (like the foreground Koin singleton) so repeated syncs don't leak an
 * [OidcAuthApi]'s underlying HTTP client.
 */
private val headlessAuthService: AuthService by lazy {
  AuthService(OAuthConfig.Default, SessionRepository, OidcAuthApi(OAuthConfig.Default))
}
