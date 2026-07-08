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
import dev.ohs.player.generated.state.GroupListState
import dev.ohs.player.reference.app.data.Extraction
import dev.ohs.player.reference.app.data.datasource.allPatientIds
import dev.ohs.player.reference.app.data.datasource.groupListSearchResults
import dev.ohs.player.reference.app.data.datasource.groupProfileSearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class InMemoryFhirRepositoryTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun upsertBundle_persistsExtractedResourcesForUiQueries() = runTest {
    val repository = InMemoryFhirRepository(seedResourcesLoader = { emptyList() })
    val bundle = json.decodeFromString(Bundle.serializer(), extractedBundleJson())

    val storedCount = repository.upsert(bundle)

    assertEquals(1L, repository.revision.value)
    assertEquals(2, storedCount)
    assertEquals(listOf("patient-100"), allPatientIds(repository))

    val groupResults = groupListSearchResults(repository)
    assertEquals(listOf("group-100"), groupResults.map { it.resource.id })

    val groupProfile = assertNotNull(groupProfileSearchResult("group-100", repository))
    val includedPatients =
      groupProfile.included?.get("member").orEmpty().filterIsInstance<Patient>()
    assertEquals(listOf("patient-100"), includedPatients.mapNotNull(Patient::id))
    assertEquals(null, groupProfile.revIncluded)
    assertTrue(repository.all("QuestionnaireResponse").isEmpty())
  }

  @Test
  fun groupProfileSearchResult_resolvesAbsolutePatientReferences() = runTest {
    val repository = InMemoryFhirRepository(seedResourcesLoader = { emptyList() })
    val patient =
      json.decodeFromString(
        Patient.serializer(),
        """
          {
            "resourceType": "Patient",
            "id": "patient-200",
            "active": true,
            "name": [
              {
                "family": "Otieno",
                "given": ["Akinyi"]
              }
            ]
          }
        """
          .trimIndent(),
      )
    val group =
      json.decodeFromString(
        Group.serializer(),
        """
          {
            "resourceType": "Group",
            "id": "group-200",
            "type": "person",
            "actual": true,
            "member": [
              {
                "entity": {
                  "reference": "https://example.org/fhir/Patient/patient-200/_history/3"
                }
              }
            ]
          }
        """
          .trimIndent(),
      )

    repository.upsert(patient)
    repository.upsert(group)

    val groupProfile = assertNotNull(groupProfileSearchResult("group-200", repository))
    val includedPatients =
      groupProfile.included?.get("member").orEmpty().filterIsInstance<Patient>()

    assertEquals(listOf("patient-200"), includedPatients.mapNotNull(Patient::id))
  }

  @Test
  fun upsert_repairsMalformedHouseholdNameFromLinkedPatient() = runTest {
    val repository = InMemoryFhirRepository(seedResourcesLoader = { emptyList() })
    val patient =
      json.decodeFromString(
        Patient.serializer(),
        """
          {
            "resourceType": "Patient",
            "id": "patient-300",
            "active": true,
            "name": [
              {
                "family": "Otieno",
                "given": ["Akinyi"]
              }
            ]
          }
        """
          .trimIndent(),
      )
    val group =
      json.decodeFromString(
        Group.serializer(),
        """
          {
            "resourceType": "Group",
            "id": "group-300",
            "type": "person",
            "actual": true,
            "name": "HumanName(id=null, extension=[], use=null)",
            "member": [
              {
                "entity": {
                  "reference": "Patient/patient-300"
                }
              }
            ]
          }
        """
          .trimIndent(),
      )

    repository.upsert(patient)
    repository.upsert(group)

    val states =
      groupListSearchResults(repository).flatMap { result ->
        Extraction.extractor.extract<GroupListState>(result)
      }

    assertEquals(listOf("Otieno Household"), states.mapNotNull { it.groupName })
  }

  @Test
  fun snapshotStore_rehydratesResourcesAcrossRepositoryInstances() = runTest {
    val snapshotStore = MemorySnapshotStore()
    val firstRepository =
      InMemoryFhirRepository(seedResourcesLoader = { emptyList() }, snapshotStore = snapshotStore)

    firstRepository.upsert(json.decodeFromString(Bundle.serializer(), extractedBundleJson()))

    val secondRepository =
      InMemoryFhirRepository(seedResourcesLoader = { emptyList() }, snapshotStore = snapshotStore)

    assertEquals(listOf("patient-100"), allPatientIds(secondRepository))
    val groups = groupListSearchResults(secondRepository)
    assertEquals(listOf("group-100"), groups.map { it.resource.id })
  }

  private fun extractedBundleJson(): String =
    """
      {
        "resourceType": "Bundle",
        "type": "collection",
        "entry": [
          {
            "fullUrl": "urn:uuid:patient-100",
            "resource": {
              "resourceType": "Patient",
              "active": true,
              "name": [
                {
                  "family": "Otieno",
                  "given": ["Akinyi"]
                }
              ],
              "gender": "female"
            }
          },
          {
            "fullUrl": "urn:uuid:group-100",
            "resource": {
              "resourceType": "Group",
              "type": "person",
              "actual": true,
              "active": true,
              "member": [
                {
                  "entity": {
                    "reference": "urn:uuid:patient-100"
                  }
                }
              ]
            }
          }
        ]
      }
    """
      .trimIndent()
}

private class MemorySnapshotStore : RepositorySnapshotStore {
  private var snapshot: String? = null

  override suspend fun read(): String? = snapshot

  override suspend fun write(snapshot: String) {
    this.snapshot = snapshot
  }
}
