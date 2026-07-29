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

import dev.ohs.fhir.engine.FhirEngine
import dev.ohs.fhir.engine.db.ResourceNotFoundException
import dev.ohs.fhir.engine.resourceType
import dev.ohs.fhir.engine.search.Search
import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.fhir.model.r4.terminologies.ResourceType
import dev.ohs.player.reference.app.data.DataChangeSignal
import dev.ohs.player.reference.app.generateId
import dev.ohs.player.reference.app.util.FhirJson
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * [FhirRepository] backed by a real on-disk database via [FhirEngine].
 *
 * Bundle submissions resolve entry ids and rewrite internal references — see
 * [normalizeBundleResources] (added in a later change).
 */
class FhirEngineRepository(private val fhirEngine: FhirEngine) : FhirRepository {

  private val json = FhirJson.instance

  override val revision: StateFlow<Long> = DataChangeSignal.revision

  override suspend fun upsert(resource: Resource) {
    upsertResource(resource)
    DataChangeSignal.notifyChanged()
  }

  override suspend fun upsert(bundle: Bundle): Int {
    val normalized = normalizeBundleResources(bundle)
    if (normalized.isEmpty()) return 0
    fhirEngine.withTransaction { normalized.forEach { upsertResource(it) } }
    DataChangeSignal.notifyChanged()
    return normalized.size
  }

  override suspend fun get(resourceType: String, id: String): Resource? {
    return runCatching { fhirEngine.get(ResourceType.valueOf(resourceType), id) }
      .getOrElse { if (it is ResourceNotFoundException) null else throw it }
  }

  override suspend fun all(resourceType: String): List<Resource> {
    return fhirEngine.search<Resource>(Search(ResourceType.valueOf(resourceType))).map {
      it.resource
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
