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

class OAuthConfigTest {

  @Test
  fun discoveryUrl_appendsWellKnown_andTrimsTrailingSlash() {
    val keycloak =
      OAuthConfig(
        issuer = "https://keycloak.example.org/realms/ohs-player/",
        clientId = "ohs-player-reference-app",
        scopes = "openid profile",
      )
    assertEquals(
      "https://keycloak.example.org/realms/ohs-player/.well-known/openid-configuration",
      keycloak.discoveryUrl,
    )

    val zitadel = keycloak.copy(issuer = "https://my-instance.zitadel.cloud")
    assertEquals(
      "https://my-instance.zitadel.cloud/.well-known/openid-configuration",
      zitadel.discoveryUrl,
    )
  }
}
