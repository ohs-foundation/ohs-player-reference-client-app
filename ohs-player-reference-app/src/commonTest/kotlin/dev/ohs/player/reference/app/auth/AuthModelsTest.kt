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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class AuthModelsTest {

  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun discoveryDocument_parsesProviderEndpoints() {
    val body =
      """
      {
        "issuer": "https://idp.example.org",
        "authorization_endpoint": "https://idp.example.org/oauth/v2/authorize",
        "token_endpoint": "https://idp.example.org/oauth/v2/token",
        "userinfo_endpoint": "https://idp.example.org/oidc/v1/userinfo",
        "end_session_endpoint": "https://idp.example.org/oidc/v1/end_session"
      }
      """
        .trimIndent()

    val doc = json.decodeFromString<OpenIdConfiguration>(body)

    assertEquals("https://idp.example.org/oauth/v2/authorize", doc.authorizationEndpoint)
    assertEquals("https://idp.example.org/oauth/v2/token", doc.tokenEndpoint)
    assertEquals("https://idp.example.org/oidc/v1/userinfo", doc.userInfoEndpoint)
    assertEquals("https://idp.example.org/oidc/v1/end_session", doc.endSessionEndpoint)
  }

  @Test
  fun discoveryDocument_toleratesMissingOptionalEndpoints() {
    val body =
      """
      {
        "authorization_endpoint": "https://idp.example.org/authorize",
        "token_endpoint": "https://idp.example.org/token"
      }
      """
        .trimIndent()

    val doc = json.decodeFromString<OpenIdConfiguration>(body)

    assertEquals("", doc.userInfoEndpoint)
    assertEquals("", doc.endSessionEndpoint)
  }

  @Test
  fun session_isAccessTokenExpired_trueOncePastExpiryMinusSkew() {
    val session =
      Session(
        accessToken = "a",
        refreshToken = "r",
        idToken = null,
        expiresInSeconds = 300,
        obtainedAtEpochSeconds = 1_000,
        user = UserInfo(),
      )

    assertEquals(false, session.isAccessTokenExpired(nowEpochSeconds = 1_269)) // 1000+300-30=1270
    assertEquals(true, session.isAccessTokenExpired(nowEpochSeconds = 1_270))
  }
}
