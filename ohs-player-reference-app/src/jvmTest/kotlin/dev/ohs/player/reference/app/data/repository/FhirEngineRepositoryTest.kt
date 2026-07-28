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
import dev.ohs.fhir.engine.FhirEngineConfiguration
import dev.ohs.fhir.engine.FhirEngineProvider
import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Group
import dev.ohs.fhir.model.r4.Patient
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class FhirEngineRepositoryTest {
  private val json = Json { ignoreUnknownKeys = true }
  private lateinit var fhirEngine: FhirEngine

  @BeforeTest
  fun setUp() = runTest {
    if (FhirEngineProvider.isNotInitialized()) {
      FhirEngineProvider.init(
        FhirEngineConfiguration(
          storageDirectory = Files.createTempDirectory("fhir-engine-repository-test").toString()
        )
      )
    }
    fhirEngine = FhirEngineProvider.getInstance()
    fhirEngine.clearDatabase()
  }

  @Test
  fun upsertResource_thenGet_returnsStoredResource() = runTest {
    val repository = FhirEngineRepository(fhirEngine)
    val patient =
      json.decodeFromString(
        Patient.serializer(),
        """
          {
            "resourceType": "Patient",
            "id": "patient-1",
            "active": true,
            "name": [{"family": "Otieno", "given": ["Akinyi"]}]
          }
        """
          .trimIndent(),
      )

    repository.upsert(patient)

    val stored = repository.get("Patient", "patient-1") as? Patient
    assertEquals("patient-1", stored?.id)
    assertEquals(1L, repository.revision.value)
  }

  @Test
  fun upsertResource_calledTwiceWithSameId_updatesInPlace() = runTest {
    val repository = FhirEngineRepository(fhirEngine)
    val original =
      json.decodeFromString(
        Patient.serializer(),
        """{"resourceType": "Patient", "id": "patient-2", "active": true}""",
      )
    val updated =
      json.decodeFromString(
        Patient.serializer(),
        """{"resourceType": "Patient", "id": "patient-2", "active": false}""",
      )

    repository.upsert(original)
    repository.upsert(updated)

    val stored = repository.get("Patient", "patient-2") as? Patient
    assertEquals(false, stored?.active?.value)
    assertEquals(listOf("patient-2"), repository.all("Patient").mapNotNull { it.id })
    assertEquals(2L, repository.revision.value)
  }

  @Test
  fun get_missingResource_returnsNull() = runTest {
    val repository = FhirEngineRepository(fhirEngine)

    assertNull(repository.get("Patient", "does-not-exist"))
  }

  @Test
  fun upsertBundle_resolvesFullUrlIdsAndRewritesReferences() = runTest {
    val repository = FhirEngineRepository(fhirEngine)
    val bundle =
      json.decodeFromString(
        Bundle.serializer(),
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
                  "name": [{"family": "Otieno", "given": ["Akinyi"]}]
                }
              },
              {
                "fullUrl": "urn:uuid:group-100",
                "resource": {
                  "resourceType": "Group",
                  "type": "person",
                  "actual": true,
                  "member": [
                    {"entity": {"reference": "urn:uuid:patient-100"}}
                  ]
                }
              }
            ]
          }
        """
          .trimIndent(),
      )

    val storedCount = repository.upsert(bundle)

    assertEquals(2, storedCount)
    assertEquals(1L, repository.revision.value)
    val patients = repository.all("Patient").filterIsInstance<Patient>()
    assertEquals(1, patients.size)
    val patientId = patients.first().id.orEmpty()
    val groups = repository.all("Group").filterIsInstance<Group>()
    assertEquals("Patient/$patientId", groups.first().member.first().entity.reference?.value)
  }

  @Test
  fun upsertBundle_resolvesIdsViaRequestUrlAbsoluteFullUrlAndGeneratedFallback() = runTest {
    val repository = FhirEngineRepository(fhirEngine)
    val bundle =
      json.decodeFromString(
        Bundle.serializer(),
        """
          {
            "resourceType": "Bundle",
            "type": "collection",
            "entry": [
              {
                "fullUrl": "https://example.org/fhir/Patient/patient-abs-1",
                "resource": {
                  "resourceType": "Patient",
                  "name": [{"family": "Abs"}]
                }
              },
              {
                "request": {"method": "PUT", "url": "Patient/patient-req-1"},
                "resource": {
                  "resourceType": "Patient",
                  "name": [{"family": "Req"}]
                }
              },
              {
                "resource": {
                  "resourceType": "Patient",
                  "name": [{"family": "Generated"}]
                }
              },
              {
                "fullUrl": "urn:uuid:group-200",
                "resource": {
                  "resourceType": "Group",
                  "type": "person",
                  "actual": true,
                  "member": [
                    {"entity": {"reference": "https://example.org/fhir/Patient/patient-abs-1"}}
                  ]
                }
              },
              {
                "fullUrl": "urn:uuid:dropped-entry"
              }
            ]
          }
        """
          .trimIndent(),
      )

    val storedCount = repository.upsert(bundle)

    // 4 stored: absolute-fullUrl patient, request-url patient, generated-id patient, and the
    // group. The 5th entry (resource == null) is silently dropped and does not count.
    assertEquals(4, storedCount)
    assertEquals(1L, repository.revision.value)

    val patients = repository.all("Patient").filterIsInstance<Patient>()
    assertEquals(3, patients.size)
    val patientIds = patients.mapNotNull { it.id }

    // idFromFullUrl: absolute-URL fullUrl resolves to the last path segment.
    assertEquals(true, patientIds.contains("patient-abs-1"))
    // idFromRequestUrl: no fullUrl, so id comes from request.url's last path segment.
    assertEquals(true, patientIds.contains("patient-req-1"))
    // generateId(): neither fullUrl nor request present, so a random id is generated. It's
    // whichever id isn't one of the two known ones above.
    val generatedId = patientIds.first { it != "patient-abs-1" && it != "patient-req-1" }
    assertEquals(true, generatedId.isNotBlank())

    // Reference rewriting: the group's reference to the absolute-fullUrl patient is rewritten to
    // "Patient/<resolved-id>", proving the plain-path idFromFullUrl branch feeds the referenceMap.
    val groups = repository.all("Group").filterIsInstance<Group>()
    assertEquals(1, groups.size)
    assertEquals("Patient/patient-abs-1", groups.first().member.first().entity.reference?.value)
  }
}
