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
package dev.ohs.player.reference.app.feature.questionnaire

import dev.ohs.fhir.datacapture.extraction.template.TemplateExtractionEngine
import dev.ohs.fhir.model.r4.AllergyIntolerance
import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Condition
import dev.ohs.fhir.model.r4.Group
import dev.ohs.fhir.model.r4.Immunization
import dev.ohs.fhir.model.r4.MedicationRequest
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Questionnaire as QuestionnaireR4
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.String as FhirString
import dev.ohs.player.reference.app.data.repository.FhirRepository
import dev.ohs.player.reference.app.generateId
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ohs_player_reference_client_app.reference_app.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Caller-supplied identifiers describing why/for-whom a questionnaire was launched. */
data class QuestionnaireLaunchContext(val patientId: String? = null, val groupId: String? = null)

/** Outcome of submitting a QuestionnaireResponse, ready for the UI to render. */
data class QuestionnaireSubmissionResult(
  val savedResourceCount: Int,
  val bundleJson: String,
  val successMessage: String,
)

/** Canonical Questionnaire.id values expected to already be persisted in the repository. */
object QuestionnaireIds {
  const val HOUSEHOLD_REGISTRATION = "household-registration"
  const val HOUSEHOLD_MEMBERS = "household-members"
  const val PATIENT_CLINICAL_DATA = "patient-clinical-data"
}

/** Bundled Questionnaire JSON, keyed by the id it should be read under. */
private val BUNDLED_QUESTIONNAIRE_PATHS: Map<String, String> =
  mapOf(
    QuestionnaireIds.HOUSEHOLD_REGISTRATION to
      "files/configs/Questionnaire-HouseholdRegistration.json",
    QuestionnaireIds.HOUSEHOLD_MEMBERS to "files/configs/Questionnaire-HouseholdMembers.json",
    QuestionnaireIds.PATIENT_CLINICAL_DATA to "files/configs/Questionnaire-PatientClinicalData.json",
  )

/** Launch-context values a questionnaire can opt into prepopulating, by linkId. */
private val LAUNCH_CONTEXT_LINK_IDS: Map<String, (QuestionnaireLaunchContext) -> String?> =
  mapOf(
    "patient-id" to { context -> context.patientId },
    "group-id" to { context -> context.groupId },
  )

/**
 * Reads/persists FHIR Questionnaire and QuestionnaireResponse data via [FhirRepository].
 *
 * Questionnaires are no longer served from bundled JSON paths - they are expected to already be
 * upserted into the repository (e.g. at app startup) and are looked up here by id.
 */
class QuestionnaireService(private val repository: FhirRepository) {

  private val fhirJson = FhirJson.instance

  /** Launch-context values a questionnaire can opt into prepopulating, by linkId. */
  private val LAUNCH_CONTEXT_LINK_IDS: Map<String, (QuestionnaireLaunchContext) -> String?> =
    mapOf(
      "patient-id" to { context -> context.patientId },
      "group-id" to { context -> context.groupId },
    )

  /**
   * Raw string placeholders that extraction templates may embed directly (e.g. inside a FHIRPath
   * reference like "Patient/__PATIENT_ID__"). These are substituted on the encoded JSON text, since
   * they aren't tied to any particular questionnaire item/linkId.
   */
  private val LAUNCH_CONTEXT_PLACEHOLDERS: Map<String, (QuestionnaireLaunchContext) -> String?> =
    mapOf(
      "__PATIENT_ID__" to { context -> context.patientId },
      "__GROUP_ID__" to { context -> context.groupId },
    )

  /** Reads a Questionnaire from the bundled config files. */
  @OptIn(ExperimentalResourceApi::class)
  suspend fun getQuestionnaire(id: String): QuestionnaireR4 {
    val path =
      BUNDLED_QUESTIONNAIRE_PATHS[id]
        ?: error("Questionnaire '$id' was not found in the app config.")
    val json = Res.readBytes(path).decodeToString()
    return fhirJson.decodeFromString(QuestionnaireR4.serializer(), json).copy(id = id)
  }

  suspend fun prepareForLaunch(
    questionnaire: QuestionnaireR4,
    launchContext: QuestionnaireLaunchContext,
  ): String {
    val questionnaireObject =
      fhirJson.decodeFromString(
        JsonObject.serializer(),
        fhirJson.encodeToString(QuestionnaireR4.serializer(), questionnaire),
      )

    val prepared =
      LAUNCH_CONTEXT_LINK_IDS.entries.fold(questionnaireObject) { current, (linkId, resolve) ->
        val value = resolve(launchContext) ?: return@fold current
        current.withInitialStringAnswer(linkId, value).first
      }

    val preparedJson = fhirJson.encodeToString(JsonObject.serializer(), prepared)

    return LAUNCH_CONTEXT_PLACEHOLDERS.entries.fold(preparedJson) { current, (placeholder, resolve)
      ->
      val value = resolve(launchContext) ?: return@fold current
      current.replace(placeholder, value)
    }
  }

  suspend fun submit(
    questionnaire: QuestionnaireR4,
    response: QuestionnaireResponse,
    launchContext: QuestionnaireLaunchContext,
  ): QuestionnaireSubmissionResult =
    when (questionnaire.id) {
      QuestionnaireIds.HOUSEHOLD_REGISTRATION ->
        submitHouseholdRegistration(questionnaire, response)

      QuestionnaireIds.HOUSEHOLD_MEMBERS ->
        submitHouseholdMembers(questionnaire, response, launchContext)

      QuestionnaireIds.PATIENT_CLINICAL_DATA ->
        submitClinicalData(questionnaire, response, launchContext)

      else -> error("No submission handling is defined for questionnaire '${questionnaire.id}'.")
    }

  private suspend fun submitHouseholdRegistration(
    questionnaire: QuestionnaireR4,
    response: QuestionnaireResponse,
  ): QuestionnaireSubmissionResult {
    val normalizedBundle =
      normalizeBundle(TemplateExtractionEngine.extract(questionnaire, response))
    val savedResourceCount = repository.upsert(normalizedBundle)

    return QuestionnaireSubmissionResult(
      savedResourceCount = savedResourceCount,
      bundleJson = fhirJson.encodeToString(Bundle.serializer(), normalizedBundle),
      successMessage =
        if (savedResourceCount > 0) {
          "Saved $savedResourceCount household resources to the in-memory repository."
        } else {
          "No household resources were extracted from this submission."
        },
    )
  }

  private suspend fun submitClinicalData(
    questionnaire: QuestionnaireR4,
    response: QuestionnaireResponse,
    launchContext: QuestionnaireLaunchContext,
  ): QuestionnaireSubmissionResult {
    val patientId =
      launchContext.patientId ?: error("A patient id is required for clinical update.")
    val patientReference = FhirString(value = "Patient/$patientId")
    val patientRef = Reference(reference = patientReference)
    val extractedBundle = TemplateExtractionEngine.extract(questionnaire, response)
    val bundle =
      extractedBundle.copy(
        entry =
          extractedBundle.entry.map { entry ->
            val updatedResource =
              when (val resource = entry.resource) {
                is AllergyIntolerance ->
                  resource.copy(id = resource.id.orGeneratedId(), patient = patientRef)

                is MedicationRequest ->
                  resource.copy(id = resource.id.orGeneratedId(), subject = patientRef)

                is Condition ->
                  resource.copy(id = resource.id.orGeneratedId(), subject = patientRef)

                is Immunization ->
                  resource.copy(id = resource.id.orGeneratedId(), patient = patientRef)

                else -> resource
              }

            entry.copy(resource = updatedResource)
          }
      )

    val savedResourceCount = repository.upsert(bundle)

    return QuestionnaireSubmissionResult(
      savedResourceCount = savedResourceCount,
      bundleJson = fhirJson.encodeToString(Bundle.serializer(), bundle),
      successMessage =
        if (savedResourceCount > 0) {
          "Saved $savedResourceCount clinical resources for patient $patientId."
        } else {
          "No clinical resources were added. Fill at least one section to update the profile."
        },
    )
  }

  private suspend fun submitHouseholdMembers(
    questionnaire: QuestionnaireR4,
    response: QuestionnaireResponse,
    launchContext: QuestionnaireLaunchContext,
  ): QuestionnaireSubmissionResult {
    val groupId = launchContext.groupId ?: error("A group id is required to add household members.")
    val existingGroup =
      repository.get("Group", groupId) as? Group
        ?: error(
          "Household $groupId was not found. Members can only be added to an existing household."
        )

    val extractedBundle = TemplateExtractionEngine.extract(questionnaire, response)
    val memberEntries =
      extractedBundle.entry.mapNotNull { entry ->
        val patient = entry.resource as? Patient ?: return@mapNotNull null
        entry.copy(resource = normalizeExtractedPatient(patient))
      }

    if (memberEntries.isEmpty()) {
      return QuestionnaireSubmissionResult(
        savedResourceCount = 0,
        bundleJson = fhirJson.encodeToString(Bundle.serializer(), extractedBundle),
        successMessage =
          "No household members were added. Complete at least one member entry to update the household.",
      )
    }

    val extractedMembers =
      memberEntries.mapNotNull { it.resource as? Patient }.map(Patient::toGroupMember)
    val membersBundle = extractedBundle.copy(entry = memberEntries)
    val savedPatientsCount = repository.upsert(membersBundle)
    repository.upsert(existingGroup.copy(member = existingGroup.member + extractedMembers))

    return QuestionnaireSubmissionResult(
      savedResourceCount = savedPatientsCount + 1,
      bundleJson = fhirJson.encodeToString(Bundle.serializer(), membersBundle),
      successMessage =
        "Added ${extractedMembers.size} household member(s) to the selected household.",
    )
  }

  private fun String?.orGeneratedId(): String = takeUnless { it.isNullOrBlank() } ?: generateId()

  private fun normalizeBundle(bundle: Bundle): Bundle {
    val normalizedEntries =
      bundle.entry.map { entry ->
        entry.copy(
          resource =
            when (val resource = entry.resource) {
              is Group -> normalizeExtractedGroup(resource, resource.id.orGeneratedId())

              is Observation -> resource.copy(id = resource.id.orGeneratedId())
              is Patient -> normalizeExtractedPatient(resource)
              else -> resource
            }
        )
      }

    val groupMembers =
      normalizedEntries.mapNotNull { it.resource as? Patient }.map(Patient::toGroupMember)

    return bundle.copy(
      entry =
        normalizedEntries.map { entry ->
          entry.copy(
            resource =
              when (val resource = entry.resource) {
                is Group -> resource.copy(member = groupMembers)
                else -> resource
              }
          )
        }
    )
  }

  private fun normalizeExtractedPatient(patient: Patient): Patient =
    patient.copy(id = patient.id.orGeneratedId())

  private fun normalizeExtractedGroup(resource: Group, groupId: String): Group {
    val householdName = resource.name?.takeIf { !it.value.isNullOrBlank() }
    return resource.copy(id = groupId, name = householdName)
  }
}

private fun Patient.toGroupMember(): Group.Member =
  Group.Member(entity = Reference(reference = FhirString(value = "Patient/$id")))

private fun JsonObject.withInitialStringAnswer(
  linkId: String,
  value: String,
): Pair<JsonObject, Boolean> {
  var updated = false
  val mutableNode = toMutableMap()

  if (this["linkId"]?.jsonPrimitive?.contentOrNull == linkId) {
    mutableNode["initial"] =
      JsonArray(listOf(JsonObject(mapOf("valueString" to JsonPrimitive(value)))))
    updated = true
  }

  this["item"]
    ?.jsonArray
    ?.map { element ->
      val (updatedItem, itemUpdated) = element.jsonObject.withInitialStringAnswer(linkId, value)
      if (itemUpdated) updated = true
      updatedItem
    }
    ?.let { mutableNode["item"] = JsonArray(it) }

  return JsonObject(mutableNode) to updated
}
