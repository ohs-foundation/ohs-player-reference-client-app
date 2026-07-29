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

import dev.ohs.fhir.engine.FhirEngine
import dev.ohs.fhir.engine.FhirEngineConfiguration
import dev.ohs.fhir.engine.FhirEngineProvider
import dev.ohs.fhir.engine.search.Search
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.terminologies.ResourceType
import dev.ohs.player.reference.app.data.repository.FhirEngineRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/** Verifies AuthViewModel.logout() actually wipes local FHIR data, using a real FhirEngine. */
class AuthViewModelLogoutTest {

  private lateinit var fhirEngine: FhirEngine

  private class FakeSessionStore(initial: Session?) : SessionStore {
    private val _session = MutableStateFlow(initial)
    override val session: StateFlow<Session?> = _session.asStateFlow()

    override suspend fun load(): Session? = _session.value

    override suspend fun save(session: Session) {
      _session.value = session
    }

    override suspend fun clear() {
      _session.value = null
    }

    override suspend fun savePending(pending: PendingAuth) = Unit

    override suspend fun takePending(): PendingAuth? = null
  }

  @BeforeTest
  fun setUp() = runTest {
    if (FhirEngineProvider.isNotInitialized()) {
      FhirEngineProvider.init(
        FhirEngineConfiguration(
          storageDirectory = Files.createTempDirectory("auth-viewmodel-logout-test").toString()
        )
      )
    }
    fhirEngine = FhirEngineProvider.getInstance()
    fhirEngine.clearDatabase()
  }

  private fun apiWithEndSessionEndpoint(): OidcAuthApi {
    val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    val discoveryBody =
      """
      {
        "authorization_endpoint": "https://idp.example.org/authorize",
        "token_endpoint": "https://idp.example.org/token",
        "userinfo_endpoint": "https://idp.example.org/userinfo",
        "end_session_endpoint": "https://idp.example.org/logout"
      }
      """
        .trimIndent()
    val engine = MockEngine { request ->
      when {
        request.url.encodedPath.endsWith("openid-configuration") ->
          respond(discoveryBody, HttpStatusCode.OK, jsonHeaders)
        request.url.encodedPath.endsWith("/logout") -> respond("", HttpStatusCode.NoContent)
        else -> respond("not found", HttpStatusCode.NotFound)
      }
    }
    val client =
      HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    return OidcAuthApi(OAuthConfig("https://idp.example.org", "client", "openid"), client)
  }

  private fun session() =
    Session(
      accessToken = "a",
      refreshToken = "r",
      idToken = null,
      expiresInSeconds = 10_000_000_000L,
      obtainedAtEpochSeconds = 0,
      user = UserInfo(),
    )

  @Test
  fun logout_clearsLocalFhirData() = runTest {
    val json = Json { ignoreUnknownKeys = true }
    val repository = FhirEngineRepository(fhirEngine)
    val patient =
      json.decodeFromString(
        Patient.serializer(),
        """{"resourceType": "Patient", "id": "patient-to-be-cleared"}""",
      )
    repository.upsert(patient)
    assertEquals(1L, fhirEngine.count(Search(ResourceType.Patient)))

    val service =
      AuthService(
        OAuthConfig("https://idp.example.org", "client", "openid"),
        FakeSessionStore(session()),
        apiWithEndSessionEndpoint(),
      )
    val viewModel = AuthViewModel(service, fhirEngine)

    viewModel.logoutForTest()

    assertEquals(0L, fhirEngine.count(Search(ResourceType.Patient)))
    assertEquals(AuthState.Unauthenticated, viewModel.state.value)
  }
}
