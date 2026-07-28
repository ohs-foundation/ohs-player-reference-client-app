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

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.kotlincrypto.hash.sha2.SHA256

/**
 * RFC 7636 (PKCE) helpers.
 *
 * The verifier is high-entropy random bytes, base64url-encoded. The challenge is
 * `BASE64URL(SHA-256(ASCII(verifier)))` (method `S256`). SHA-256 comes from KotlinCrypto so it runs
 * synchronously on every target — including Wasm/JS, where the browser's SubtleCrypto digest is
 * async-only.
 */
internal data class PkcePair(val verifier: String, val challenge: String) {
  val method: String = "S256"
}

internal object Pkce {

  /** 32 random bytes → 43-char verifier, comfortably inside the 43–128 range. */
  @OptIn(ExperimentalEncodingApi::class)
  fun generate(): PkcePair {
    val verifier = base64Url(secureRandomBytes(32))
    val challenge = base64Url(SHA256().digest(verifier.encodeToByteArray()))
    return PkcePair(verifier = verifier, challenge = challenge)
  }

  /** A random, URL-safe `state` value to defend against CSRF on the callback. */
  fun randomState(): String = base64Url(secureRandomBytes(16))

  @OptIn(ExperimentalEncodingApi::class)
  private fun base64Url(bytes: ByteArray): String =
    Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(bytes)
}

/**
 * Cryptographically secure random bytes. Implemented per platform with the OS CSPRNG (SecureRandom
 * / SecRandomCopyBytes / Web Crypto getRandomValues).
 */
internal expect fun secureRandomBytes(size: Int): ByteArray
