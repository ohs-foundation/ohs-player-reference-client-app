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

import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.fhir.resourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** [FhirRepository] test double, seeded from the bundled sample resources for UI tests. */
class InMemorySampleFhirRepository : FhirRepository {
  private val resourcesByType = mutableMapOf<String, MutableMap<String, Resource>>()
  private val seedMutex = Mutex()
  private var seeded = false
  private val _revision = MutableStateFlow(0L)

  override val revision: StateFlow<Long> = _revision

  override suspend fun upsert(resource: Resource) {
    ensureSeeded()
    store(resource)
    _revision.value += 1
  }

  override suspend fun upsert(bundle: Bundle): Int {
    ensureSeeded()
    val resources = bundle.entry.mapNotNull { it.resource }
    resources.forEach(::store)
    if (resources.isNotEmpty()) _revision.value += 1
    return resources.size
  }

  override suspend fun get(resourceType: String, id: String): Resource? {
    ensureSeeded()
    return resourcesByType[resourceType]?.get(id)
  }

  override suspend fun all(resourceType: String): List<Resource> {
    ensureSeeded()
    return resourcesByType[resourceType]?.values?.toList().orEmpty()
  }

  private suspend fun ensureSeeded() {
    if (seeded) return
    seedMutex.withLock {
      if (seeded) return@withLock
      loadBundledSampleResources().forEach(::store)
      seeded = true
    }
  }

  private fun store(resource: Resource) {
    val id = resource.id ?: return
    resourcesByType.getOrPut(resource.resourceType) { mutableMapOf() }[id] = resource
  }
}
