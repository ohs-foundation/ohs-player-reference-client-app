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
package dev.ohs.player.reference.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import dev.ohs.fhir.engine.sync.FhirDataStore
import dev.ohs.fhir.engine.sync.SyncJobStatus
import dev.ohs.fhir.engine.sync.createDataStore
import dev.ohs.player.reference.app.auth.AuthService
import dev.ohs.player.reference.app.auth.OAuthConfig
import dev.ohs.player.reference.app.auth.OidcAuthApi
import dev.ohs.player.reference.app.auth.PendingAuth
import dev.ohs.player.reference.app.auth.Session
import dev.ohs.player.reference.app.auth.SessionStore
import dev.ohs.player.reference.app.data.di.repositoryModule
import dev.ohs.player.reference.app.data.di.viewModelModule
import dev.ohs.player.reference.app.data.repository.FhirRepository
import dev.ohs.player.reference.app.data.repository.InMemorySampleFhirRepository
import dev.ohs.player.reference.app.data.sync.SyncNowUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

private class FakeAppSyncNowUseCase : SyncNowUseCase {
  override suspend fun invoke(): SyncJobStatus = SyncJobStatus.Succeeded()
}

private class InMemorySessionStore(initial: Session? = null) : SessionStore {
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

@OptIn(ExperimentalTestApi::class)
class AppAuthGatingTest {

  private fun newFhirDataStore(): FhirDataStore {
    val path = Files.createTempFile("app-auth-gating-test", ".preferences_pb").toString()
    return FhirDataStore(createDataStore { path })
  }

  private fun startTestKoin() {
    val engine = MockEngine { respond("not found", HttpStatusCode.NotFound) }
    val client =
      HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    startKoin {
      modules(
        module {
          single<FhirRepository> { InMemorySampleFhirRepository() }
          single { newFhirDataStore() }
          single<SyncNowUseCase> { FakeAppSyncNowUseCase() }
          single { OAuthConfig("https://idp.example.org", "client", "openid") }
          single<SessionStore> { InMemorySessionStore(initial = null) }
          single { OidcAuthApi(get(), client) }
          single { AuthService(get(), get(), get()) }
        },
        repositoryModule,
        viewModelModule,
      )
    }
  }

  @AfterTest fun tearDown() = stopKoin()

  @Test
  fun signedOut_showsLoginScreen() = runComposeUiTest {
    startTestKoin()

    setContent { App() }

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Sign in", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
    }
    assertTrue(onAllNodesWithText("Sign in", ignoreCase = true).fetchSemanticsNodes().isNotEmpty())
  }
}
