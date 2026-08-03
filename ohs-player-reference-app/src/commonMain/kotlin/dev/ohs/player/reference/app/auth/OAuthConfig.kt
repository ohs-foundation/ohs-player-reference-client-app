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
 * Provider-agnostic OAuth2/OIDC settings.
 *
 * Only the [issuer] (plus client id and scopes) is configured; the concrete endpoints are resolved
 * at runtime via OIDC discovery ([OidcEndpoints]) from `{issuer}/.well-known/openid-configuration`.
 * The redirect URI is intentionally NOT part of this object — it is platform specific and provided
 * by the [AuthorizationLauncher]. [Default] is the single instance, baked in from
 * `local.properties` via [GeneratedAuthConfig].
 */
data class OAuthConfig(
  /**
   * OIDC issuer, e.g. a Keycloak realm `https://host/realms/ohs-player` or
   * `https://x.zitadel.cloud`.
   */
  val issuer: String,
  val clientId: String,
  /** Space-separated OAuth scopes, e.g. "openid profile email offline_access". */
  val scopes: String,
) {
  /** Standard OIDC discovery document URL for this issuer. */
  val discoveryUrl: String
    get() = "${issuer.trimEnd('/')}/.well-known/openid-configuration"

  companion object {
    val Default: OAuthConfig =
      OAuthConfig(
        issuer = GeneratedAuthConfig.ISSUER,
        clientId = GeneratedAuthConfig.CLIENT_ID,
        scopes = GeneratedAuthConfig.SCOPES,
      )
  }
}

/** OAuth/OIDC endpoints resolved from the provider's discovery document. */
data class OidcEndpoints(
  val authorizationEndpoint: String,
  val tokenEndpoint: String,
  val userInfoEndpoint: String,
  /** May be blank: not every provider advertises an end-session endpoint. */
  val endSessionEndpoint: String,
)
