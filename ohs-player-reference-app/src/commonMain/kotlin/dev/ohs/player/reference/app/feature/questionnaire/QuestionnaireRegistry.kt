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
import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Group
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Questionnaire as QuestionnaireR4
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.player.reference.app.data.repository.FhirRepository
import dev.ohs.player.reference.app.generateId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class QuestionnaireDefinition(
  val id: String,
  val title: String,
  val questionnairePath: String,
  val prepareQuestionnaireJson: suspend QuestionnairePreparationContext.() -> String = {
    sourceQuestionnaireJson
  },
  val buildLaunchContextMap: suspend QuestionnaireLaunchContext.() -> Map<String, String> = {
    emptyMap()
  },
  val submit: suspend QuestionnaireSubmissionContext.() -> QuestionnaireSubmissionResult,
)

data class QuestionnaireLaunchContext(val patientId: String? = null, val groupId: String? = null)

data class QuestionnairePreparationContext(
  val sourceQuestionnaireJson: String,
  val launchContext: QuestionnaireLaunchContext,
  val fhirJson: Json,
)

data class QuestionnaireSubmissionContext(
  val questionnaireJson: String,
  val response: QuestionnaireResponse,
  val launchContext: QuestionnaireLaunchContext,
  val repository: FhirRepository,
  val fhirJson: Json,
)

data class QuestionnaireSubmissionResult(
  val savedResourceCount: Int,
  val bundleJson: String,
  val successMessage: String,
)

object QuestionnaireRegistry {
  const val HOUSEHOLD_REGISTRATION_ID = "household-registration"
  const val HOUSEHOLD_MEMBERS_ID = "household-members"
  const val PATIENT_CLINICAL_DATA_ID = "patient-clinical-data"

  val householdRegistration =
    QuestionnaireDefinition(
      id = HOUSEHOLD_REGISTRATION_ID,
      title = "Household registration",
      questionnairePath = "files/configs/Questionnaire-HouseholdRegistration.json",
      submit = {
        val questionnaire =
          fhirJson.decodeFromString(QuestionnaireR4.serializer(), questionnaireJson)
        val bundle = TemplateExtractionEngine.extract(questionnaire, response)
        val householdName = response.findStringAnswer("household-name", fhirJson)
        val headFamilyName = response.findStringAnswer("head-family-name", fhirJson)
        val groupMembers = mutableListOf<Group.Member>()
        val updatedEntries =
          bundle.entry.map { entry ->
            val resource = entry.resource
            val updatedResource =
              when (resource) {
                is Group -> {
                  normalizeExtractedGroup(
                    resource = resource,
                    groupId = generateId(),
                    householdName = householdName,
                    headFamilyName = headFamilyName,
                    fhirJson = fhirJson,
                  )
                }

                is Observation -> {
                  resource.copy(id = generateId())
                }

                is Patient -> {
                  normalizeExtractedPatient(resource, groupMembers)
                }

                else -> resource
              }
            entry.copy(resource = updatedResource)
          }
        val finalUpdatedEntries =
          updatedEntries.map { entry ->
            val resource = entry.resource

            val finalResource =
              when (resource) {
                is Group -> {
                  // Guarantee the Group itself has an ID, and append the accumulated members
                  val finalGroupId = if (resource.id.isNullOrBlank()) generateId() else resource.id
                  resource.copy(id = finalGroupId, member = groupMembers)
                }

                else -> resource
              }
            entry.copy(resource = finalResource)
          }

        val modifiedBundle = bundle.copy(entry = finalUpdatedEntries)
        val savedResourceCount = repository.upsert(modifiedBundle)
        QuestionnaireSubmissionResult(
          savedResourceCount = savedResourceCount,
          bundleJson = fhirJson.encodeToString(Bundle.serializer(), modifiedBundle),
          successMessage =
            if (savedResourceCount > 0) {
              "Saved $savedResourceCount household resources to the in-memory repository."
            } else {
              "No household resources were extracted from this submission."
            },
        )
      },
    )

  val patientClinicalData =
    QuestionnaireDefinition(
      id = PATIENT_CLINICAL_DATA_ID,
      title = "Clinical update",
      questionnairePath = "files/configs/Questionnaire-PatientClinicalData.json",
      prepareQuestionnaireJson = {
        val resolvedPatientId =
          launchContext.patientId ?: error("A patient id is required for clinical update.")
        injectInitialStringAnswer(
            questionnaireJson = sourceQuestionnaireJson,
            linkId = "patient-id",
            value = resolvedPatientId,
            json = this.fhirJson,
          )
          .replace("__PATIENT_ID__", resolvedPatientId)
      },
      submit = {
        val resolvedPatientId =
          launchContext.patientId ?: error("A patient id is required for clinical update.")
        require(repository.get("Patient", resolvedPatientId) is Patient) {
          "Patient $resolvedPatientId was not found. Clinical data can only be added to an existing patient."
        }
        val questionnaire =
          fhirJson.decodeFromString(QuestionnaireR4.serializer(), questionnaireJson)
        val bundle = TemplateExtractionEngine.extract(questionnaire, response)
        val savedResourceCount = repository.upsert(bundle)
        QuestionnaireSubmissionResult(
          savedResourceCount = savedResourceCount,
          bundleJson = fhirJson.encodeToString(Bundle.serializer(), bundle),
          successMessage =
            if (savedResourceCount > 0) {
              "Saved $savedResourceCount clinical resources for patient $resolvedPatientId."
            } else {
              "No clinical resources were added. Fill at least one section to update the profile."
            },
        )
      },
    )

  val householdMembers =
    QuestionnaireDefinition(
      id = HOUSEHOLD_MEMBERS_ID,
      title = "Add household members",
      questionnairePath = "files/configs/Questionnaire-HouseholdMembers.json",
      submit = {
        val resolvedGroupId =
          launchContext.groupId ?: error("A group id is required to add household members.")
        val existingGroup =
          repository.get("Group", resolvedGroupId) as? Group
            ?: error(
              "Household $resolvedGroupId was not found. Members can only be added to an existing household."
            )
        val questionnaire =
          fhirJson.decodeFromString(QuestionnaireR4.serializer(), questionnaireJson)
        val extractedBundle = TemplateExtractionEngine.extract(questionnaire, response)
        val extractedMembers = mutableListOf<Group.Member>()
        val memberEntries =
          extractedBundle.entry.mapNotNull { entry ->
            val patient = entry.resource as? Patient ?: return@mapNotNull null
            entry.copy(resource = normalizeExtractedPatient(patient, extractedMembers))
          }

        if (memberEntries.isEmpty()) {
          QuestionnaireSubmissionResult(
            savedResourceCount = 0,
            bundleJson = fhirJson.encodeToString(Bundle.serializer(), extractedBundle),
            successMessage =
              "No household members were added. Complete at least one member entry to update the household.",
          )
        } else {
          val membersBundle = extractedBundle.copy(entry = memberEntries)
          val savedPatientsCount = repository.upsert(membersBundle)
          repository.upsert(existingGroup.copy(member = existingGroup.member + extractedMembers))

          QuestionnaireSubmissionResult(
            savedResourceCount = savedPatientsCount + 1,
            bundleJson = fhirJson.encodeToString(Bundle.serializer(), membersBundle),
            successMessage =
              "Added ${extractedMembers.size} household member(s) to the selected household.",
          )
        }
      },
    )

  private val definitions = listOf(householdRegistration, householdMembers, patientClinicalData)

  fun find(id: String): QuestionnaireDefinition? = definitions.firstOrNull { it.id == id }
}

private fun normalizeExtractedPatient(
  resource: Patient,
  groupMembers: MutableList<Group.Member>,
): Patient {
  val finalId = if (resource.id.isNullOrBlank()) generateId() else resource.id
  groupMembers.add(
    Group.Member(
      entity = Reference(reference = dev.ohs.fhir.model.r4.String(value = "Patient/$finalId"))
    )
  )
  return resource.copy(id = finalId)
}

private fun injectInitialStringAnswer(
  questionnaireJson: String,
  linkId: String,
  value: String,
  json: Json,
): String {
  val questionnaire = json.decodeFromString(JsonObject.serializer(), questionnaireJson)
  val (updatedQuestionnaire, updated) = questionnaire.withInitialStringAnswer(linkId, value)
  require(updated) { "Questionnaire item '$linkId' was not found in the questionnaire." }
  return json.encodeToString(JsonObject.serializer(), updatedQuestionnaire)
}

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

  val childItems =
    this["item"]?.jsonArray?.map { itemElement ->
      val itemObject = itemElement.jsonObject
      val (updatedItem, itemUpdated) = itemObject.withInitialStringAnswer(linkId, value)
      if (itemUpdated) updated = true
      updatedItem
    }

  childItems?.let { mutableNode["item"] = JsonArray(it) }

  return JsonObject(mutableNode) to updated
}

private fun normalizeExtractedGroup(
  resource: Group,
  groupId: String,
  householdName: String?,
  headFamilyName: String?,
  fhirJson: Json,
): Group {
  val resourceJson = fhirJson.encodeToJsonElement(Group.serializer(), resource).jsonObject
  val resolvedName =
    householdName?.trim()?.takeIf { it.isNotBlank() }
      ?: headFamilyName?.trim()?.takeIf { it.isNotBlank() }?.let { "$it Household" }

  val patchedJson =
    buildMap<String, JsonElement> {
      putAll(resourceJson)
      put("id", JsonPrimitive(groupId))
      resolvedName?.let { put("name", JsonPrimitive(it)) }
    }

  return fhirJson.decodeFromJsonElement(Group.serializer(), JsonObject(patchedJson))
}

private fun QuestionnaireResponse.findStringAnswer(linkId: String, json: Json): String? =
  json
    .encodeToJsonElement(QuestionnaireResponse.serializer(), this)
    .jsonObject
    .findStringAnswer(linkId)

private fun JsonObject.findStringAnswer(linkId: String): String? {
  if (this["linkId"]?.jsonPrimitive?.contentOrNull == linkId) {
    return (this["answer"] as? JsonArray)
      ?.firstOrNull()
      ?.jsonObject
      ?.get("valueString")
      ?.jsonPrimitive
      ?.contentOrNull
  }

  return (this["item"] as? JsonArray)?.firstNotNullOfOrNull { itemElement ->
    itemElement.jsonObject.findStringAnswer(linkId)
  }
}
