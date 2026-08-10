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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.kotlincrypto.hash.sha2.SHA256

class PkceTest {

  @OptIn(ExperimentalEncodingApi::class)
  @Test
  fun pkceChallenge_isBase64UrlSha256OfVerifier() {
    val pair = Pkce.generate()

    assertEquals("S256", pair.method)
    assertTrue(pair.verifier.length in 43..128, "verifier length ${pair.verifier.length}")

    val expected =
      Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
        .encode(SHA256().digest(pair.verifier.encodeToByteArray()))
    assertEquals(expected, pair.challenge, "challenge must be S256 of verifier")

    listOf(pair.verifier, pair.challenge).forEach {
      assertFalse(it.contains('+') || it.contains('/') || it.contains('='), "not url-safe: $it")
    }
  }

  @Test
  fun verifiersAndStates_areUnique() {
    assertNotEquals(Pkce.generate().verifier, Pkce.generate().verifier)
    assertNotEquals(Pkce.randomState(), Pkce.randomState())
  }
}
