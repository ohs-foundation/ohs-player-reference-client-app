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
package dev.ohs.player.reference.app.data.repository

import dev.ohs.fhir.FhirEngine
import dev.ohs.fhir.db.ResourceNotFoundException
import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.fhir.model.r4.terminologies.ResourceType
import dev.ohs.fhir.resourceType
import dev.ohs.fhir.search.Search
import dev.ohs.player.reference.app.generateId
import dev.ohs.player.reference.app.util.FhirJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * [FhirRepository] backed by a real on-disk database via [FhirEngine].
 *
 * On first access, seeds the bundled sample resources if the database is empty — see
 * [ensureSeeded]. Bundle submissions resolve entry ids and rewrite internal references — see
 * [normalizeBundleResources] (added in a later change).
 */
class FhirEngineRepository(
  private val fhirEngine: FhirEngine,
  private val seedResourcesLoader: suspend () -> List<Resource> = ::loadBundledSampleResources,
) : FhirRepository {

  private val json = FhirJson.instance
  private val seedMutex = Mutex()
  private var seeded = false
  private val _revision = MutableStateFlow(0L)

  override val revision: StateFlow<Long> = _revision

  override suspend fun upsert(resource: Resource) {
    ensureSeeded()
    upsertResource(resource)
    _revision.value += 1
  }

  override suspend fun upsert(bundle: Bundle): Int {
    ensureSeeded()
    return 0
  }

  override suspend fun get(resourceType: String, id: String): Resource? {
    ensureSeeded()
    return runCatching { fhirEngine.get(ResourceType.valueOf(resourceType), id) }
      .getOrElse { if (it is ResourceNotFoundException) null else throw it }
  }

  override suspend fun all(resourceType: String): List<Resource> {
    ensureSeeded()
    return fhirEngine.search<Resource>(Search(ResourceType.valueOf(resourceType))).map {
      it.resource
    }
  }

  private suspend fun ensureSeeded() {
    if (seeded) return
    seedMutex.withLock { seeded = true }
  }

  private suspend fun upsertResource(resource: Resource) {
    val withId = if (resource.id == null) resource.withId(generateId()) else resource
    val type = ResourceType.valueOf(withId.resourceType)
    val exists = runCatching { fhirEngine.get(type, withId.id!!) }.isSuccess
    if (exists) fhirEngine.update(withId) else fhirEngine.create(withId)
  }

  private fun Resource.withId(newId: String): Resource {
    val obj = json.encodeToJsonElement(Resource.serializer(), this).jsonObject
    return json.decodeFromJsonElement(
      Resource.serializer(),
      kotlinx.serialization.json.JsonObject(obj + ("id" to JsonPrimitive(newId))),
    )
  }
}

private suspend fun loadBundledSampleResources(): List<Resource> = emptyList()
