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
import dev.ohs.fhir.engine.FhirEngine
import dev.ohs.fhir.engine.LocalChange
import dev.ohs.fhir.engine.OffsetDateTime
import dev.ohs.fhir.engine.SearchResult
import dev.ohs.fhir.engine.db.LocalChangeResourceReference
import dev.ohs.fhir.engine.search.Search
import dev.ohs.fhir.engine.sync.ConflictResolver
import dev.ohs.fhir.engine.sync.FhirDataStore
import dev.ohs.fhir.engine.sync.SyncJobStatus
import dev.ohs.fhir.engine.sync.createDataStore
import dev.ohs.fhir.engine.sync.upload.SyncUploadProgress
import dev.ohs.fhir.engine.sync.upload.UploadRequestResult
import dev.ohs.fhir.engine.sync.upload.UploadStrategy
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.fhir.model.r4.terminologies.ResourceType
import dev.ohs.player.reference.app.auth.AuthService
import dev.ohs.player.reference.app.auth.OAuthConfig
import dev.ohs.player.reference.app.auth.OidcAuthApi
import dev.ohs.player.reference.app.auth.PendingAuth
import dev.ohs.player.reference.app.auth.Session
import dev.ohs.player.reference.app.auth.SessionStore
import dev.ohs.player.reference.app.auth.UserInfo
import dev.ohs.player.reference.app.data.di.repositoryModule
import dev.ohs.player.reference.app.data.di.viewModelModule
import dev.ohs.player.reference.app.data.repository.FhirRepository
import dev.ohs.player.reference.app.data.repository.InMemorySampleFhirRepository
import dev.ohs.player.reference.app.data.sync.PeriodicSyncUseCase
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

private class FakeAppSyncNowUseCase : SyncNowUseCase {
  var invocationCount = 0
    private set

  override suspend fun invoke(): SyncJobStatus {
    invocationCount++
    return SyncJobStatus.Succeeded()
  }

  override suspend fun cancel() {}
}

private class FakeAppPeriodicSyncUseCase : PeriodicSyncUseCase {
  override suspend fun start() {}

  override suspend fun cancel() {}
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

/** Satisfies AuthViewModel's constructor; no test in this file calls logout(). */
private class NoOpFhirEngine : FhirEngine {
  override suspend fun create(vararg resource: Resource): List<String> =
    error("not used in this test")

  override suspend fun get(type: ResourceType, id: String): Resource =
    error("not used in this test")

  override suspend fun update(vararg resource: Resource) = error("not used in this test")

  override suspend fun delete(type: ResourceType, id: String) = error("not used in this test")

  override suspend fun <R : Resource> search(search: Search): List<SearchResult<R>> =
    error("not used in this test")

  override suspend fun syncUpload(
    uploadStrategy: UploadStrategy,
    upload:
      suspend (List<LocalChange>, List<LocalChangeResourceReference>) -> Flow<UploadRequestResult>,
  ): Flow<SyncUploadProgress> = error("not used in this test")

  override suspend fun syncDownload(
    conflictResolver: ConflictResolver,
    download: suspend () -> Flow<List<Resource>>,
  ) = error("not used in this test")

  override suspend fun count(search: Search): Long = error("not used in this test")

  override suspend fun getLastSyncTimeStamp(): OffsetDateTime? = error("not used in this test")

  override suspend fun clearDatabase() = error("not used in this test")

  override suspend fun getLocalChanges(type: ResourceType, id: String): List<LocalChange> =
    error("not used in this test")

  override suspend fun purge(type: ResourceType, id: String, forcePurge: Boolean) =
    error("not used in this test")

  override suspend fun purge(type: ResourceType, ids: Set<String>, forcePurge: Boolean) =
    error("not used in this test")

  override suspend fun withTransaction(block: suspend FhirEngine.() -> Unit) =
    error("not used in this test")
}

@OptIn(ExperimentalTestApi::class)
class AppAuthGatingTest {

  private fun newFhirDataStore(): FhirDataStore {
    val path = Files.createTempFile("app-auth-gating-test", ".preferences_pb").toString()
    return FhirDataStore(createDataStore { path })
  }

  private fun testSession() =
    Session(
      accessToken = "a",
      refreshToken = "r",
      idToken = null,
      expiresInSeconds = 10_000_000_000L,
      obtainedAtEpochSeconds = 0,
      user = UserInfo(),
    )

  private fun testPatient(): Patient =
    Json { ignoreUnknownKeys = true }
      .decodeFromString(Patient.serializer(), """{"resourceType": "Patient", "id": "p1"}""")

  private fun startTestKoin(
    session: Session? = null,
    fhirRepository: FhirRepository = InMemorySampleFhirRepository(),
    syncNowUseCase: SyncNowUseCase = FakeAppSyncNowUseCase(),
  ) {
    val engine = MockEngine { respond("not found", HttpStatusCode.NotFound) }
    val client =
      HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    startKoin {
      modules(
        module {
          single<FhirRepository> { fhirRepository }
          single { newFhirDataStore() }
          single<SyncNowUseCase> { syncNowUseCase }
          single<PeriodicSyncUseCase> { FakeAppPeriodicSyncUseCase() }
          single { OAuthConfig("https://idp.example.org", "client", "openid") }
          single<SessionStore> { InMemorySessionStore(initial = session) }
          single { OidcAuthApi(get(), client) }
          single { AuthService(get(), get(), get()) }
          single<FhirEngine> { NoOpFhirEngine() }
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

  @Test
  fun authenticatedWithNoLocalData_runsSyncThenShowsHome() = runComposeUiTest {
    val syncNowUseCase = FakeAppSyncNowUseCase()
    startTestKoin(session = testSession(), syncNowUseCase = syncNowUseCase)

    setContent { App() }

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Sync now", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
    }
    assertEquals(1, syncNowUseCase.invocationCount)
  }

  @Test
  fun authenticatedWithExistingLocalData_skipsSyncAndShowsHomeImmediately() = runComposeUiTest {
    val repository = InMemorySampleFhirRepository()
    runBlocking { repository.upsert(testPatient()) }
    val syncNowUseCase = FakeAppSyncNowUseCase()
    startTestKoin(
      session = testSession(),
      fhirRepository = repository,
      syncNowUseCase = syncNowUseCase,
    )

    setContent { App() }

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Sync now", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
    }
    assertEquals(0, syncNowUseCase.invocationCount)
  }
}
