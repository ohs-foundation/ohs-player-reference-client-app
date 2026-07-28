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

import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Storage seam for the session + pending-auth state. Lets tests substitute an in-memory fake. */
internal interface SessionStore {
  val session: StateFlow<Session?>

  suspend fun load(): Session?

  suspend fun save(session: Session)

  suspend fun clear()

  suspend fun savePending(pending: PendingAuth)

  suspend fun takePending(): PendingAuth?
}

/**
 * Single source of truth for the signed-in [Session] and the transient [PendingAuth] used during
 * the authorization round-trip. Everything is persisted through [KSafe], which encrypts at rest
 * using each platform's hardware-backed keystore (Android Keystore, Apple Keychain, OS vaults,
 * non-extractable WebCrypto keys on web).
 *
 * KSafe's `get` needs a default, so we use blank sentinels and treat a blank token / verifier as
 * "absent".
 *
 * Deliberately a plain `object`, not Koin-constructed: [FhirBearerAuthenticator] must read it from
 * each platform's `main()` / `Application.onCreate()`, which runs before `initKoin()`. Koin binds
 * this same singleton via `single<SessionStore> { SessionRepository }` for everything else.
 */
internal object SessionRepository : SessionStore {

  private val ksafe: KSafe by lazy { createKSafe() }

  private val _session = MutableStateFlow<Session?>(null)
  /** Emits the current session, or null when signed out. Hydrate via [load]. */
  override val session: StateFlow<Session?> = _session.asStateFlow()

  /** Reads any persisted session into [session]. Call once at startup. */
  override suspend fun load(): Session? = readSession().also { _session.value = it }

  override suspend fun save(session: Session) {
    ksafe.put(KEY_SESSION, session)
    _session.value = session
  }

  override suspend fun clear() {
    ksafe.put(KEY_SESSION, EMPTY_SESSION)
    _session.value = null
  }

  override suspend fun savePending(pending: PendingAuth) = ksafe.put(KEY_PENDING, pending)

  /** Returns the pending auth (if any) and clears it — single use. */
  override suspend fun takePending(): PendingAuth? {
    val pending = ksafe.get(KEY_PENDING, EMPTY_PENDING)
    ksafe.put(KEY_PENDING, EMPTY_PENDING)
    return pending.takeIf { it.codeVerifier.isNotBlank() }
  }

  private suspend fun readSession(): Session? =
    ksafe.get(KEY_SESSION, EMPTY_SESSION).takeIf { it.accessToken.isNotBlank() }

  private const val KEY_SESSION = "auth.session"
  private const val KEY_PENDING = "auth.pending"

  private val EMPTY_SESSION =
    Session(
      accessToken = "",
      refreshToken = null,
      idToken = null,
      expiresInSeconds = 0,
      obtainedAtEpochSeconds = 0,
      user = UserInfo(),
    )
  private val EMPTY_PENDING = PendingAuth(codeVerifier = "", state = "")
}

/** Creates the platform [KSafe] (Android needs the app Context; others don't). */
internal expect fun createKSafe(): KSafe
