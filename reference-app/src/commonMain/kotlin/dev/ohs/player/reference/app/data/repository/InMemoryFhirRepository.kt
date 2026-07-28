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
import dev.ohs.fhir.model.r4.Group
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.player.reference.app.data.patientIdFromReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import player_reference.reference_app.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * TODO(#58): Temporary workaround until FHIREngine integration is available. Remove this once
 *   FHIREngine owns persistence and reference handling; that should simplify this implementation.
 */
class InMemoryFhirRepository(
  private val seedResourcesLoader: suspend () -> List<Resource> = ::loadBundledSampleResources,
  private val snapshotStore: RepositorySnapshotStore? = null,
) : FhirRepository {
  private val mutex = Mutex()
  private val resourcesByType = mutableMapOf<String, MutableMap<String, Resource>>()
  private val fhirJson = Json { ignoreUnknownKeys = true }
  private var initialized = false
  private var generatedIdCounter = 0L
  private val _revision = MutableStateFlow(0L)

  override val revision: StateFlow<Long> = _revision

  override suspend fun upsert(resource: Resource) {
    ensureInitialized()
    mutex.withLock {
      val normalized = normalizeResource(resource)
      if (storeResource(normalized) != null) {
        repairStoredGroups()
        persistSnapshotLocked()
        _revision.value += 1
      }
    }
  }

  override suspend fun upsert(bundle: Bundle): Int {
    ensureInitialized()
    return mutex.withLock {
      val normalizedResources = normalizeBundleResources(bundle)
      normalizedResources.forEach(::storeResource)
      if (normalizedResources.isNotEmpty()) {
        repairStoredGroups()
        persistSnapshotLocked()
        _revision.value += 1
      }
      normalizedResources.size
    }
  }

  override suspend fun get(resourceType: String, id: String): Resource? {
    ensureInitialized()
    return mutex.withLock { resourcesByType[resourceType]?.get(id) }
  }

  override suspend fun all(resourceType: String): List<Resource> {
    ensureInitialized()
    return mutex.withLock { resourcesByType[resourceType]?.values?.toList().orEmpty() }
  }

  private suspend fun ensureInitialized() {
    if (initialized) return
    mutex.withLock {
      if (initialized) return
      seedResourcesLoader().forEach(::storeResource)
      loadPersistedResources().forEach(::storeResource)
      if (repairStoredGroups()) {
        persistSnapshotLocked()
      }
      initialized = true
    }
  }

  private fun storeResource(resource: Resource): Resource? {
    val resourceJson = fhirJson.encodeToJsonElement(Resource.serializer(), resource).jsonObject
    val resourceType = resourceJson["resourceType"]?.jsonPrimitive?.contentOrNull ?: return null
    val id = resourceJson["id"]?.jsonPrimitive?.contentOrNull ?: return null
    val bucket = resourcesByType.getOrPut(resourceType) { mutableMapOf() }
    bucket[id] = resource
    return resource
  }

  private fun normalizeResource(resource: Resource): Resource {
    val resourceJson = fhirJson.encodeToJsonElement(Resource.serializer(), resource).jsonObject
    val resourceType = resourceJson["resourceType"]?.jsonPrimitive?.contentOrNull ?: return resource
    val id = resourceJson["id"]?.jsonPrimitive?.contentOrNull ?: generateResourceId(resourceType)
    return decodeResource(JsonObject(resourceJson + ("id" to JsonPrimitive(id))))
  }

  private fun normalizeBundleResources(bundle: Bundle): List<Resource> {
    val drafts =
      bundle.entry.mapNotNull { entry ->
        val resource = entry.resource ?: return@mapNotNull null
        val resourceJson = fhirJson.encodeToJsonElement(Resource.serializer(), resource).jsonObject
        val resourceType =
          resourceJson["resourceType"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val normalizedId =
          resourceJson["id"]?.jsonPrimitive?.contentOrNull
            ?: idFromFullUrl(entry.fullUrl?.value)
            ?: idFromRequestUrl(entry.request?.url?.value, resourceType)
            ?: generateResourceId(resourceType)
        ResourceDraft(
          resourceType = resourceType,
          normalizedId = normalizedId,
          fullUrl = entry.fullUrl?.value,
          resourceJson = JsonObject(resourceJson + ("id" to JsonPrimitive(normalizedId))),
        )
      }

    val referenceMap =
      drafts
        .mapNotNull { draft ->
          draft.fullUrl?.let { fullUrl -> fullUrl to "${draft.resourceType}/${draft.normalizedId}" }
        }
        .toMap()

    return drafts.map { draft ->
      decodeResource(rewriteReferences(draft.resourceJson, referenceMap).jsonObject)
    }
  }

  private fun decodeResource(resourceJson: JsonObject): Resource =
    fhirJson.decodeFromJsonElement(Resource.serializer(), JsonObject(resourceJson))

  private suspend fun loadPersistedResources(): List<Resource> {
    val snapshot = snapshotStore?.read().orEmpty()
    if (snapshot.isBlank()) return emptyList()
    return runCatching {
        fhirJson.decodeFromString(ListSerializer(Resource.serializer()), snapshot)
      }
      .getOrElse { emptyList() }
  }

  private suspend fun persistSnapshotLocked() {
    val snapshotStore = snapshotStore ?: return
    val snapshot =
      fhirJson.encodeToString(
        ListSerializer(Resource.serializer()),
        resourcesByType.values
          .flatMap { it.values }
          .sortedWith(compareBy({ resourceTypeName(it) }, { resourceId(it).orEmpty() })),
      )
    snapshotStore.write(snapshot)
  }

  private fun rewriteReferences(
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
              rewriteReferences(value, referenceMap)
            }
          }
        )

      is JsonArray -> JsonArray(element.map { rewriteReferences(it, referenceMap) })
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

  private fun generateResourceId(resourceType: String): String {
    generatedIdCounter += 1
    return "${resourceType.lowercase()}-$generatedIdCounter"
  }

  private fun repairStoredGroups(): Boolean {
    val groupBucket = resourcesByType["Group"] ?: return false
    var repairedAny = false

    groupBucket.entries.toList().forEach { (groupId, resource) ->
      val repaired = repairGroupResource(resource)
      if (repaired != resource) {
        groupBucket[groupId] = repaired
        repairedAny = true
      }
    }

    return repairedAny
  }

  private fun repairGroupResource(resource: Resource): Resource {
    val group = resource as? Group ?: return resource
    val groupJson = fhirJson.encodeToJsonElement(Group.serializer(), group).jsonObject
    val repairedMemberArray = repairMemberReferences(groupJson["member"] as? JsonArray)
    val normalizedGroupJson =
      if (repairedMemberArray != null) JsonObject(groupJson + ("member" to repairedMemberArray))
      else groupJson

    val currentName = normalizedGroupJson["name"]?.jsonPrimitive?.contentOrNull?.trim()
    val repairedName =
      if (!currentName.isNullOrBlank() && !looksLikeModelDump(currentName)) {
        currentName
      } else {
        inferredHouseholdName(normalizedGroupJson)
      }

    val updates =
      buildMap<String, JsonElement> {
        repairedMemberArray?.let { put("member", it) }
        repairedName?.let { put("name", JsonPrimitive(it)) }
      }
    if (updates.isEmpty()) return resource

    val repairedJson = JsonObject(normalizedGroupJson + updates)
    return fhirJson.decodeFromJsonElement(Group.serializer(), repairedJson)
  }

  private fun repairMemberReferences(memberArray: JsonArray?): JsonArray? {
    memberArray ?: return null
    var repairedAny = false

    val repairedMembers =
      memberArray.map { memberElement ->
        val memberJson = memberElement as? JsonObject ?: return@map memberElement
        val entityJson = memberJson["entity"] as? JsonObject ?: return@map memberElement
        val existingReference = entityJson["reference"]?.jsonPrimitive?.contentOrNull

        if (!existingReference.isNullOrBlank()) return@map memberElement

        val malformedReferenceId =
          (((entityJson["_reference"] as? JsonObject)?.get("id")) as? JsonPrimitive)
            ?.contentOrNull
            ?.trim()
            .orEmpty()
        if (malformedReferenceId.isBlank()) return@map memberElement

        repairedAny = true
        JsonObject(
          memberJson +
            ("entity" to
              JsonObject(entityJson + ("reference" to JsonPrimitive(malformedReferenceId))))
        )
      }

    return if (repairedAny) JsonArray(repairedMembers) else null
  }

  private fun inferredHouseholdName(groupJson: JsonObject): String? {
    val memberArray = groupJson["member"] as? JsonArray ?: return null
    return memberArray.firstNotNullOfOrNull { memberElement ->
      val memberJson = memberElement as? JsonObject ?: return@firstNotNullOfOrNull null
      val reference =
        ((memberJson["entity"] as? JsonObject)?.get("reference") as? JsonPrimitive)?.contentOrNull
      val patientId = patientIdFromReference(reference) ?: return@firstNotNullOfOrNull null
      val patient =
        resourcesByType["Patient"]?.get(patientId) as? Patient ?: return@firstNotNullOfOrNull null
      val patientJson = fhirJson.encodeToJsonElement(Patient.serializer(), patient).jsonObject
      val firstName = (patientJson["name"] as? JsonArray)?.firstOrNull() as? JsonObject
      val familyName = firstName?.get("family")?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
      val givenName =
        ((firstName?.get("given") as? JsonArray)?.firstOrNull() as? JsonPrimitive)
          ?.contentOrNull
          ?.trim()
          .orEmpty()

      when {
        familyName.isNotBlank() -> "$familyName Household"
        givenName.isNotBlank() -> "$givenName Household"
        else -> null
      }
    }
  }

  private fun looksLikeModelDump(value: String): Boolean =
    value.startsWith("HumanName(") ||
      value.contains("dev.ohs.fhir.model.r4.") ||
      value.contains("id=null") ||
      value == "null"

  private fun resourceTypeName(resource: Resource): String =
    fhirJson
      .encodeToJsonElement(Resource.serializer(), resource)
      .jsonObject["resourceType"]
      ?.jsonPrimitive
      ?.contentOrNull
      .orEmpty()

  private fun resourceId(resource: Resource): String? =
    fhirJson
      .encodeToJsonElement(Resource.serializer(), resource)
      .jsonObject["id"]
      ?.jsonPrimitive
      ?.contentOrNull
}

private data class ResourceDraft(
  val resourceType: String,
  val normalizedId: String,
  val fullUrl: String?,
  val resourceJson: JsonObject,
)

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadBundledSampleResources(): List<Resource> {
  val fhirJson = Json { ignoreUnknownKeys = true }
  val bundleJson = Res.readBytes("files/SampleResourcesBundle.json").decodeToString()
  val bundle = fhirJson.decodeFromString(Bundle.serializer(), bundleJson)
  return bundle.entry.mapNotNull { it.resource }
}
