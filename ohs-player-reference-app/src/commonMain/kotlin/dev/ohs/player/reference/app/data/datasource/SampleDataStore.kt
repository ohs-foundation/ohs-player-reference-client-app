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
package dev.ohs.player.reference.app.data.datasource

import dev.ohs.fhir.model.r4.AllergyIntolerance
import dev.ohs.fhir.model.r4.Condition
import dev.ohs.fhir.model.r4.Group
import dev.ohs.fhir.model.r4.Immunization
import dev.ohs.fhir.model.r4.MedicationRequest
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.player.library.model.SearchResult
import dev.ohs.player.reference.app.data.AppDependencies
import dev.ohs.player.reference.app.data.patientIdFromReference
import dev.ohs.player.reference.app.data.repository.FhirRepository

/** Returns all patient IDs — used by the patient list screen. */
suspend fun allPatientIds(
  repository: FhirRepository = AppDependencies.fhirRepository
): List<String> = repository.all("Patient").mapNotNull { (it as? Patient)?.id }

/**
 * Patient list: root = Patient only. No clinical resources needed — the list card shows summary
 * fields available directly on the Patient resource.
 */
suspend fun patientSummarySearchResult(
  patientId: String,
  repository: FhirRepository = AppDependencies.fhirRepository,
): SearchResult<Resource>? {
  val patient = repository.get("Patient", patientId) as? Patient ?: return null
  return SearchResult(resource = patient)
}

/**
 * Patient profile: root = Patient, all clinical resources in revIncluded. Mirrors a real `GET
 * /Patient/{id}/$everything` response. All section extractors run against this single result.
 */
suspend fun patientProfileSearchResult(
  patientId: String,
  repository: FhirRepository = AppDependencies.fhirRepository,
): SearchResult<Resource>? {
  val patient = repository.get("Patient", patientId) as? Patient ?: return null
  val revIncluded = buildMap {
    repository
      .all("AllergyIntolerance")
      .filterIsInstance<AllergyIntolerance>()
      .filter { patientId(it.patient.reference?.value) == patientId }
      .takeIf { it.isNotEmpty() }
      ?.let { put("AllergyIntolerance" to "patient", it) }
    repository
      .all("MedicationRequest")
      .filterIsInstance<MedicationRequest>()
      .filter { patientId(it.subject.reference?.value) == patientId }
      .takeIf { it.isNotEmpty() }
      ?.let { put("MedicationRequest" to "subject", it) }
    repository
      .all("Condition")
      .filterIsInstance<Condition>()
      .filter { patientId(it.subject.reference?.value) == patientId }
      .takeIf { it.isNotEmpty() }
      ?.let { put("Condition" to "subject", it) }
    repository
      .all("Immunization")
      .filterIsInstance<Immunization>()
      .filter { patientId(it.patient.reference?.value) == patientId }
      .takeIf { it.isNotEmpty() }
      ?.let { put("Immunization" to "patient", it) }
  }
  return SearchResult(
    resource = patient,
    included = mapOf("patient" to listOf(patient)),
    revIncluded = revIncluded.ifEmpty { null },
  )
}

/**
 * Group list: root = Group only. Member count is derived from `Group.member.size` on the resource
 * itself — no additional includes needed.
 */
suspend fun groupListSearchResults(
  repository: FhirRepository
): List<SearchResult<Resource>> =
  repository.all("Group").filterIsInstance<Group>().map { group -> SearchResult(resource = group) }

/**
 * Group profile: root = Group, member Patients in included. Each `Group.member.entity` is a FHIR
 * Reference, so we normalize common reference shapes before resolving the local Patient id. The
 * head is inferred from the ordered member list, while other members can carry their relationship
 * directly on the Patient resource.
 */
suspend fun groupProfileSearchResult(
  groupId: String,
  repository: FhirRepository,
): SearchResult<Resource>? {
  val group = repository.get("Group", groupId) as? Group ?: return null
  val memberPatients =
    group.member.mapNotNull { member ->
      member.entity.reference?.value?.let(::patientIdFromReference)?.let { patientId ->
        repository.get("Patient", patientId) as? Patient
      }
    }

  return SearchResult(
    resource = group,
    included = if (memberPatients.isNotEmpty()) mapOf("member" to memberPatients) else null,
  )
}

private fun patientId(reference: String?) = patientIdFromReference(reference)
