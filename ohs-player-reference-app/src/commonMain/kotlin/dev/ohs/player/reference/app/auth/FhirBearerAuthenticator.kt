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

import dev.ohs.fhir.engine.sync.HttpAuthenticationMethod
import dev.ohs.fhir.engine.sync.HttpAuthenticator

/**
 * Feeds the signed-in session's access token to `ohs-fhir-engine`'s HTTP layer. Reads
 * [SessionRepository] directly (not through Koin) — this is constructed at each platform's
 * `FhirEngineProvider.init()` call site, which runs before `initKoin()`.
 *
 * [getAuthenticationMethod] is called by the engine on every request, so this always reads the
 * *current* token — there's no ordering dependency on when a session becomes available.
 */
internal object FhirBearerAuthenticator : HttpAuthenticator {
  override fun getAuthenticationMethod(): HttpAuthenticationMethod =
    HttpAuthenticationMethod.Bearer(SessionRepository.session.value?.accessToken.orEmpty())
}
