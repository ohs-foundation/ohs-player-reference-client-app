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
package dev.ohs.player.reference.app.data.di

import dev.ohs.fhir.engine.FhirEngine
import dev.ohs.fhir.engine.LocalChange
import dev.ohs.fhir.engine.OffsetDateTime
import dev.ohs.fhir.engine.SearchResult
import dev.ohs.fhir.engine.db.LocalChangeResourceReference
import dev.ohs.fhir.engine.search.Search
import dev.ohs.fhir.engine.sync.ConflictResolver
import dev.ohs.fhir.engine.sync.upload.SyncUploadProgress
import dev.ohs.fhir.engine.sync.upload.UploadRequestResult
import dev.ohs.fhir.engine.sync.upload.UploadStrategy
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.fhir.model.r4.terminologies.ResourceType
import dev.ohs.player.reference.app.auth.AuthService
import dev.ohs.player.reference.app.auth.AuthViewModel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

/** Satisfies AuthViewModel's constructor; this test never calls logout(). */
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

class AuthModuleTest : KoinTest {

  private val authService by inject<AuthService>()

  @AfterTest fun tearDown() = stopKoin()

  @Test
  fun authModule_resolvesAuthServiceAndAuthViewModel() {
    startKoin {
      modules(module { single<FhirEngine> { NoOpFhirEngine() } }, authModule, viewModelModule)
    }

    authService // resolves without throwing
    getKoin().get<AuthViewModel>()
  }
}
