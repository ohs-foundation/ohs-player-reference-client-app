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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

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
    val normalized = normalizeBundleResources(bundle)
    if (normalized.isEmpty()) return 0
    fhirEngine.withTransaction { normalized.forEach { upsertResource(it) } }
    _revision.value += 1
    return normalized.size
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
    seedMutex.withLock {
      if (seeded) return@withLock
      if (fhirEngine.count(Search(ResourceType.Patient)) == 0L) {
        val resources = seedResourcesLoader()
        if (resources.isNotEmpty()) {
          fhirEngine.withTransaction { resources.forEach { upsertResource(it) } }
        }
      }
      seeded = true
    }
  }

  private suspend fun upsertResource(resource: Resource) {
    val withId = if (resource.id == null) resource.withId(generateId()) else resource
    val type = ResourceType.valueOf(withId.resourceType)
    val exists = runCatching { fhirEngine.get(type, withId.id!!) }.isSuccess
    if (exists) fhirEngine.update(withId) else fhirEngine.create(withId)
  }

  private fun normalizeBundleResources(bundle: Bundle): List<Resource> {
    val drafts =
      bundle.entry.mapNotNull { entry ->
        val resource = entry.resource ?: return@mapNotNull null
        val resolvedId =
          resource.id
            ?: idFromFullUrl(entry.fullUrl?.value)
            ?: idFromRequestUrl(entry.request?.url?.value, resource.resourceType)
            ?: generateId()
        entry.fullUrl?.value to resource.withId(resolvedId)
      }

    val referenceMap =
      drafts
        .mapNotNull { (fullUrl, resource) ->
          fullUrl?.let { it to "${resource.resourceType}/${resource.id}" }
        }
        .toMap()

    return drafts.map { (_, resource) -> rewriteReferences(resource, referenceMap) }
  }

  private fun rewriteReferences(resource: Resource, referenceMap: Map<String, String>): Resource {
    val rewritten =
      rewriteReferencesInElement(
        json.encodeToJsonElement(Resource.serializer(), resource),
        referenceMap,
      )
    return json.decodeFromJsonElement(Resource.serializer(), rewritten)
  }

  private fun rewriteReferencesInElement(
    element: JsonElement,
    referenceMap: Map<String, String>,
  ): JsonElement =
    when (element) {
      is JsonObject ->
        JsonObject(
          element.mapValues { (key, value) ->
            if (key == "reference" && value is JsonPrimitive) {
              referenceMap[value.content]?.let(::JsonPrimitive) ?: value
            } else {
              rewriteReferencesInElement(value, referenceMap)
            }
          }
        )

      is JsonArray -> JsonArray(element.map { rewriteReferencesInElement(it, referenceMap) })

      else -> element
    }

  private fun idFromFullUrl(fullUrl: String?): String? =
    when {
      fullUrl.isNullOrBlank() -> null
      fullUrl.startsWith("urn:uuid:") -> fullUrl.substringAfterLast(':').ifBlank { null }
      fullUrl.contains('/') -> fullUrl.substringAfterLast('/').substringBefore('?').ifBlank { null }
      else -> null
    }

  private fun idFromRequestUrl(url: String?, resourceType: String): String? {
    if (url.isNullOrBlank()) return null
    val candidate =
      url.substringAfterLast('/').substringBefore('?').ifBlank {
        return null
      }
    return candidate.takeUnless { it == resourceType }
  }

  private fun Resource.withId(newId: String): Resource {
    val obj = json.encodeToJsonElement(Resource.serializer(), this).jsonObject
    return json.decodeFromJsonElement(
      Resource.serializer(),
      JsonObject(obj + ("id" to JsonPrimitive(newId))),
    )
  }
}

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadBundledSampleResources(): List<Resource> {
  val bundleJson = Res.readBytes("files/SampleResourcesBundle.json").decodeToString()
  val bundle = FhirJson.instance.decodeFromString(Bundle.serializer(), bundleJson)
  return bundle.entry.mapNotNull { it.resource }
}
