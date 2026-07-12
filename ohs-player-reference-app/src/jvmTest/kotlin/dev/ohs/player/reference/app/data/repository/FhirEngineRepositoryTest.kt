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
import dev.ohs.fhir.FhirEngineConfiguration
import dev.ohs.fhir.FhirEngineProvider
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
    val repository = FhirEngineRepository(fhirEngine, seedResourcesLoader = { emptyList() })
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
    val repository = FhirEngineRepository(fhirEngine, seedResourcesLoader = { emptyList() })
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
    val repository = FhirEngineRepository(fhirEngine, seedResourcesLoader = { emptyList() })

    assertNull(repository.get("Patient", "does-not-exist"))
  }
}
