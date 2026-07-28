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

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class SessionRepositoryTest {

  @AfterTest
  fun tearDown() = runTest { SessionRepository.clear() }

  private fun testSession() =
    Session(
      accessToken = "access-1",
      refreshToken = "refresh-1",
      idToken = "id-1",
      expiresInSeconds = 300,
      obtainedAtEpochSeconds = 1_000,
      user = UserInfo(subject = "u1", username = "jdoe"),
    )

  @Test
  fun saveThenLoad_roundTripsTheSession() = runTest {
    SessionRepository.save(testSession())

    val loaded = SessionRepository.load()

    assertEquals(testSession(), loaded)
    assertEquals(testSession(), SessionRepository.session.value)
  }

  @Test
  fun clear_removesTheSession() = runTest {
    SessionRepository.save(testSession())

    SessionRepository.clear()

    assertNull(SessionRepository.load())
    assertNull(SessionRepository.session.value)
  }

  @Test
  fun takePending_returnsAndClearsPendingAuth_onlyOnce() = runTest {
    SessionRepository.savePending(PendingAuth(codeVerifier = "verifier-1", state = "state-1"))

    val first = SessionRepository.takePending()
    val second = SessionRepository.takePending()

    assertEquals(PendingAuth("verifier-1", "state-1"), first)
    assertNull(second, "pending auth must be single-use")
  }
}
