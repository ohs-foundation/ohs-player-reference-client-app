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
import dev.ohs.fhir.model.r4.Questionnaire as QuestionnaireR4
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.player.reference.app.data.repository.InMemoryFhirRepository
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PatientClinicalQuestionnaireExtractionTest {
  private val json = Json {
    prettyPrint = true
    explicitNulls = false
    encodeDefaults = false
    ignoreUnknownKeys = true
  }

  @Test
  fun prepareQuestionnaireJson_seedsHiddenPatientId() = runTest {
    val preparedQuestionnaireJson =
      QuestionnaireRegistry.patientClinicalData.prepareQuestionnaireJson(
        QuestionnairePreparationContext(
          sourceQuestionnaireJson = loadClinicalQuestionnaireJson(),
          launchContext = QuestionnaireLaunchContext(patientId = "patient-123"),
          fhirJson = json,
        )
      )

    val questionnaireJson =
      json.decodeFromString(JsonObject.serializer(), preparedQuestionnaireJson)
    val patientIdItem =
      questionnaireJson["item"]
        ?.jsonArray
        ?.map { it.jsonObject }
        ?.firstOrNull { it["linkId"]?.jsonPrimitive?.content == "patient-id" }

    assertNotNull(patientIdItem)
    assertEquals(
      "patient-123",
      patientIdItem["initial"]
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("valueString")
        ?.jsonPrimitive
        ?.content,
    )
  }

  @Test
  fun clinicalQuestionnaire_extractsAndPersistsMultipleResources() = runTest {
    val preparedQuestionnaireJson =
      QuestionnaireRegistry.patientClinicalData.prepareQuestionnaireJson(
        QuestionnairePreparationContext(
          sourceQuestionnaireJson = loadClinicalQuestionnaireJson(),
          launchContext = QuestionnaireLaunchContext(patientId = "patient-123"),
          fhirJson = json,
        )
      )
    val questionnaire =
      json.decodeFromString(QuestionnaireR4.serializer(), preparedQuestionnaireJson)
    val response =
      json.decodeFromString(QuestionnaireResponse.serializer(), clinicalQuestionnaireResponseJson())
    val bundle = TemplateExtractionEngine.extract(questionnaire, response)
    val repository = InMemoryFhirRepository(seedResourcesLoader = { emptyList() })

    assertTrue(TemplateExtractionEngine.canExtract(questionnaire))
    assertEquals(5, bundle.entry.size)
    assertEquals(5, repository.upsert(bundle))

    val storedAllergies =
      repository.all("AllergyIntolerance").map { resource ->
        json.encodeToJsonElement(Resource.serializer(), resource).jsonObject
      }
    val peanutAllergy =
      storedAllergies.firstOrNull { resource ->
        resource["code"]
          ?.jsonObject
          ?.get("coding")
          ?.jsonArray
          ?.firstOrNull()
          ?.jsonObject
          ?.get("display")
          ?.jsonPrimitive
          ?.content == "Peanuts"
      }

    assertEquals(2, storedAllergies.size)
    assertNotNull(peanutAllergy)
    assertEquals(
      "Patient/patient-123",
      peanutAllergy["patient"]?.jsonObject?.get("reference")?.jsonPrimitive?.content,
    )
    assertEquals(
      "active",
      peanutAllergy["clinicalStatus"]
        ?.jsonObject
        ?.get("coding")
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("code")
        ?.jsonPrimitive
        ?.content,
    )
    assertEquals(2, peanutAllergy["reaction"]?.jsonArray?.size)
    assertEquals(
      "Hives",
      peanutAllergy["reaction"]
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("manifestation")
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("coding")
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("display")
        ?.jsonPrimitive
        ?.content,
    )

    val storedMedication =
      repository.all("MedicationRequest").single().let { resource ->
        json.encodeToJsonElement(Resource.serializer(), resource).jsonObject
      }
    assertEquals(
      "Patient/patient-123",
      storedMedication["subject"]?.jsonObject?.get("reference")?.jsonPrimitive?.content,
    )
    assertEquals("active", storedMedication["status"]?.jsonPrimitive?.content)
    assertEquals(
      "Amoxicillin",
      storedMedication["medicationCodeableConcept"]
        ?.jsonObject
        ?.get("coding")
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("display")
        ?.jsonPrimitive
        ?.content,
    )

    val storedCondition =
      repository.all("Condition").single().let { resource ->
        json.encodeToJsonElement(Resource.serializer(), resource).jsonObject
      }
    assertEquals(
      "Patient/patient-123",
      storedCondition["subject"]?.jsonObject?.get("reference")?.jsonPrimitive?.content,
    )
    assertEquals("2024-01-02", storedCondition["onsetDateTime"]?.jsonPrimitive?.content)

    val storedImmunization =
      repository.all("Immunization").single().let { resource ->
        json.encodeToJsonElement(Resource.serializer(), resource).jsonObject
      }
    assertEquals(
      "Patient/patient-123",
      storedImmunization["patient"]?.jsonObject?.get("reference")?.jsonPrimitive?.content,
    )
    assertEquals("completed", storedImmunization["status"]?.jsonPrimitive?.content)
    assertEquals("2025-10-12", storedImmunization["occurrenceDateTime"]?.jsonPrimitive?.content)
  }

  private fun loadClinicalQuestionnaireJson(): String {
    val candidates =
      listOf(
        Path.of(
          "ohs-player-reference-app",
          "src",
          "commonMain",
          "composeResources",
          "files",
          "configs",
          "Questionnaire-PatientClinicalData.json",
        ),
        Path.of(
          "src",
          "commonMain",
          "composeResources",
          "files",
          "configs",
          "Questionnaire-PatientClinicalData.json",
        ),
      )

    return candidates.firstOrNull(Files::exists)?.let(Files::readString)
      ?: error(
        "Questionnaire-PatientClinicalData.json was not found from ${System.getProperty("user.dir")}."
      )
  }

  private fun clinicalQuestionnaireResponseJson(): String =
    """
      {
        "resourceType": "QuestionnaireResponse",
        "questionnaire": "https://example.org/fhir/Questionnaire/patient-clinical-data-capture",
        "status": "completed",
        "item": [
          {
            "linkId": "patient-id",
            "answer": [
              {
                "valueString": "patient-123"
              }
            ]
          },
          {
            "linkId": "page-allergies",
            "item": [
              {
                "linkId": "allergies",
                "item": [
                  {
                    "linkId": "allergy-substance",
                    "answer": [
                      {
                        "valueString": "Peanuts"
                      }
                    ]
                  },
                  {
                    "linkId": "allergy-status",
                    "answer": [
                      {
                        "valueCoding": {
                          "system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical",
                          "code": "active",
                          "display": "Active"
                        }
                      }
                    ]
                  },
                  {
                    "linkId": "allergy-criticality",
                    "answer": [
                      {
                        "valueCoding": {
                          "code": "high",
                          "display": "High"
                        }
                      }
                    ]
                  },
                  {
                    "linkId": "allergy-reactions",
                    "item": [
                      {
                        "linkId": "allergy-reaction-manifestation",
                        "answer": [
                          {
                            "valueString": "Hives"
                          }
                        ]
                      },
                      {
                        "linkId": "allergy-reaction-severity",
                        "answer": [
                          {
                            "valueCoding": {
                              "code": "mild",
                              "display": "Mild"
                            }
                          }
                        ]
                      }
                    ]
                  },
                  {
                    "linkId": "allergy-reactions",
                    "item": [
                      {
                        "linkId": "allergy-reaction-manifestation",
                        "answer": [
                          {
                            "valueString": "Shortness of breath"
                          }
                        ]
                      },
                      {
                        "linkId": "allergy-reaction-severity",
                        "answer": [
                          {
                            "valueCoding": {
                              "code": "severe",
                              "display": "Severe"
                            }
                          }
                        ]
                      }
                    ]
                  }
                ]
              },
              {
                "linkId": "allergies",
                "item": [
                  {
                    "linkId": "allergy-substance",
                    "answer": [
                      {
                        "valueString": "Dust"
                      }
                    ]
                  },
                  {
                    "linkId": "allergy-status",
                    "answer": [
                      {
                        "valueCoding": {
                          "system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical",
                          "code": "inactive",
                          "display": "Inactive"
                        }
                      }
                    ]
                  },
                  {
                    "linkId": "allergy-criticality",
                    "answer": [
                      {
                        "valueCoding": {
                          "code": "low",
                          "display": "Low"
                        }
                      }
                    ]
                  }
                ]
              }
            ]
          },
          {
            "linkId": "page-medications",
            "item": [
              {
                "linkId": "medications",
                "item": [
                  {
                    "linkId": "medication-name",
                    "answer": [
                      {
                        "valueString": "Amoxicillin"
                      }
                    ]
                  },
                  {
                    "linkId": "medication-status",
                    "answer": [
                      {
                        "valueCoding": {
                          "code": "active",
                          "display": "Active"
                        }
                      }
                    ]
                  },
                  {
                    "linkId": "medication-dosage",
                    "answer": [
                      {
                        "valueString": "500 mg twice daily"
                      }
                    ]
                  }
                ]
              }
            ]
          },
          {
            "linkId": "page-conditions",
            "item": [
              {
                "linkId": "conditions",
                "item": [
                  {
                    "linkId": "condition-name",
                    "answer": [
                      {
                        "valueString": "Asthma"
                      }
                    ]
                  },
                  {
                    "linkId": "condition-status",
                    "answer": [
                      {
                        "valueCoding": {
                          "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
                          "code": "active",
                          "display": "Active"
                        }
                      }
                    ]
                  },
                  {
                    "linkId": "condition-onset-date",
                    "answer": [
                      {
                        "valueDate": "2024-01-02"
                      }
                    ]
                  }
                ]
              }
            ]
          },
          {
            "linkId": "page-immunizations",
            "item": [
              {
                "linkId": "immunizations",
                "item": [
                  {
                    "linkId": "immunization-name",
                    "answer": [
                      {
                        "valueString": "Influenza vaccine"
                      }
                    ]
                  },
                  {
                    "linkId": "immunization-status",
                    "answer": [
                      {
                        "valueCoding": {
                          "code": "completed",
                          "display": "Completed"
                        }
                      }
                    ]
                  },
                  {
                    "linkId": "immunization-occurrence-date",
                    "answer": [
                      {
                        "valueDate": "2025-10-12"
                      }
                    ]
                  }
                ]
              }
            ]
          }
        ]
      }
    """
      .trimIndent()
}
