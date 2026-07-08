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

import dev.ohs.fhir.model.r4.Group
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.player.reference.app.data.AppDependencies
import dev.ohs.player.reference.app.data.repository.FhirRepository
import dev.ohs.player.reference.app.data.repository.GroupRepository
import dev.ohs.player.reference.app.data.repository.InMemoryFhirRepository
import dev.ohs.player.reference.app.data.repository.PatientRepository
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class QuestionnaireProfileIntegrationTest {
  private val json = Json {
    prettyPrint = true
    explicitNulls = false
    encodeDefaults = false
    ignoreUnknownKeys = true
  }

  @Test
  fun householdRegistration_submit_exposesHeadAndMembersInGroupProfile() = runTest {
    val repository = InMemoryFhirRepository(seedResourcesLoader = { emptyList() })

    withRepository(repository) {
      val questionnaireJson = loadQuestionnaireJson("Questionnaire-HouseholdRegistration.json")
      val response =
        json.decodeFromString(
          dev.ohs.fhir.model.r4.QuestionnaireResponse.serializer(),
          householdRegistrationResponseJson(),
        )

      val result =
        QuestionnaireRegistry.householdRegistration.submit(
          QuestionnaireSubmissionContext(
            questionnaireJson = questionnaireJson,
            response = response,
            launchContext = QuestionnaireLaunchContext(),
            repository = repository,
            fhirJson = json,
          )
        )

      assertEquals(4, result.savedResourceCount)

      val savedGroup = repository.all("Group").single() as Group
      val groupId = assertNotNull(savedGroup.id)
      val profile = GroupRepository.getGroupProfile(groupId)
      val groups = GroupRepository.getGroups()

      assertEquals("Doe Household", profile.groupHeader?.groupName)
      assertEquals("Jane", profile.groupHeader?.headGivenName)
      assertEquals("Doe", profile.groupHeader?.headFamilyName)
      assertEquals(listOf("Doe Household"), groups.mapNotNull { it.groupName })
      assertEquals(
        listOf("Jane", "Junior", "John"),
        profile.members.mapNotNull { it.memberGivenName },
      )
      assertEquals(listOf("CHILD", "SPS"), profile.members.mapNotNull { it.relationshipCode })
    }
  }

  @Test
  fun patientClinicalData_submit_persistsResourcesVisibleThroughPatientProfile() = runTest {
    val repository = InMemoryFhirRepository(seedResourcesLoader = { emptyList() })
    repository.upsert(
      patient(patientId = "patient-123", givenName = "Amina", familyName = "Diallo")
    )

    withRepository(repository) {
      val questionnaireJson =
        QuestionnaireRegistry.patientClinicalData.prepareQuestionnaireJson(
          QuestionnairePreparationContext(
            sourceQuestionnaireJson =
              loadQuestionnaireJson("Questionnaire-PatientClinicalData.json"),
            launchContext = QuestionnaireLaunchContext(patientId = "patient-123"),
            fhirJson = json,
          )
        )
      val response =
        json.decodeFromString(
          dev.ohs.fhir.model.r4.QuestionnaireResponse.serializer(),
          clinicalQuestionnaireResponseJson(),
        )

      val result =
        QuestionnaireRegistry.patientClinicalData.submit(
          QuestionnaireSubmissionContext(
            questionnaireJson = questionnaireJson,
            response = response,
            launchContext = QuestionnaireLaunchContext(patientId = "patient-123"),
            repository = repository,
            fhirJson = json,
          )
        )

      assertEquals(5, result.savedResourceCount)

      val profile = PatientRepository.getPatientProfile("patient-123")

      assertEquals("Amina", profile.patient?.givenName)
      assertEquals("Diallo", profile.patient?.familyName)
      assertEquals(listOf("Peanuts", "Dust"), profile.allergies.mapNotNull { it.substance })
      assertEquals(
        listOf("Hives", "Shortness of breath"),
        profile.allergyReactions.mapNotNull { it.manifestation },
      )
      assertEquals(listOf("Amoxicillin"), profile.medications.mapNotNull { it.medicationName })
      assertEquals(listOf("Asthma"), profile.conditions.mapNotNull { it.conditionCode })
      assertEquals(listOf("Influenza vaccine"), profile.immunizations.mapNotNull { it.vaccineName })
    }
  }

  @Test
  fun householdMembers_submit_appendsMembersToExistingHouseholdGroup() = runTest {
    val repository = InMemoryFhirRepository(seedResourcesLoader = { emptyList() })
    repository.upsert(patient(patientId = "head-1", givenName = "Grace", familyName = "Banda"))
    repository.upsert(
      group(groupId = "household-1", name = "Banda Household", memberIds = listOf("head-1"))
    )

    withRepository(repository) {
      val questionnaireJson = loadQuestionnaireJson("Questionnaire-HouseholdMembers.json")
      val response =
        json.decodeFromString(
          dev.ohs.fhir.model.r4.QuestionnaireResponse.serializer(),
          householdMembersResponseJson(),
        )

      val result =
        QuestionnaireRegistry.householdMembers.submit(
          QuestionnaireSubmissionContext(
            questionnaireJson = questionnaireJson,
            response = response,
            launchContext = QuestionnaireLaunchContext(groupId = "household-1"),
            repository = repository,
            fhirJson = json,
          )
        )

      assertEquals(3, result.savedResourceCount)

      val savedGroup = repository.get("Group", "household-1") as Group
      assertEquals(3, savedGroup.member.size)
      assertEquals(
        listOf(true, true, true),
        savedGroup.member.map { it.entity.reference?.value?.startsWith("Patient/") == true },
      )

      val profile = GroupRepository.getGroupProfile("household-1")

      assertEquals("Banda Household", profile.groupHeader?.groupName)
      assertEquals("Grace", profile.groupHeader?.headGivenName)
      assertEquals("Banda", profile.groupHeader?.headFamilyName)
      assertEquals(
        listOf("Grace", "Ivy", "Noah"),
        profile.members.mapNotNull { it.memberGivenName },
      )
      assertEquals(listOf("CHILD", "EXT"), profile.members.mapNotNull { it.relationshipCode })
    }
  }

  private suspend fun withRepository(repository: FhirRepository, block: suspend () -> Unit) {
    val previous = AppDependencies.fhirRepository
    AppDependencies.fhirRepository = repository
    try {
      block()
    } finally {
      AppDependencies.fhirRepository = previous
    }
  }

  private fun loadQuestionnaireJson(fileName: String): String {
    val candidates =
      listOf(
        Path.of(
          "ohs-player-reference-app",
          "src",
          "commonMain",
          "composeResources",
          "files",
          "configs",
          fileName,
        ),
        Path.of("src", "commonMain", "composeResources", "files", "configs", fileName),
      )

    return candidates.firstOrNull(Files::exists)?.let(Files::readString)
      ?: error("$fileName was not found from ${System.getProperty("user.dir")}.")
  }

  private fun patient(patientId: String, givenName: String, familyName: String): Patient =
    json.decodeFromString(
      Patient.serializer(),
      """
        {
          "resourceType": "Patient",
          "id": "$patientId",
          "active": true,
          "name": [
            {
              "family": "$familyName",
              "given": ["$givenName"]
            }
          ],
          "gender": "female",
          "birthDate": "1990-03-14"
        }
      """
        .trimIndent(),
    )

  private fun group(groupId: String, name: String, memberIds: List<String>): Group =
    json.decodeFromString(
      Group.serializer(),
      """
        {
          "resourceType": "Group",
          "id": "$groupId",
          "type": "person",
          "actual": true,
          "active": true,
          "name": "$name",
          "member": [
            ${memberIds.joinToString(",\n            ") { """{ "entity": { "reference": "Patient/$it" } }""" }}
          ]
        }
      """
        .trimIndent(),
    )

  private fun householdRegistrationResponseJson(): String =
    """
      {
        "resourceType": "QuestionnaireResponse",
        "questionnaire": "https://example.org/fhir/Questionnaire/household-registration-template-extract",
        "status": "completed",
        "item": [
          {
            "linkId": "page-household",
            "item": [
              {
                "linkId": "household-details",
                "item": [
                  {
                    "linkId": "household-id",
                    "answer": [{ "valueString": "HH-001" }]
                  },
                  {
                    "linkId": "household-name",
                    "answer": [{ "valueString": "Doe Household" }]
                  }
                ]
              }
            ]
          },
          {
            "linkId": "page-household-head",
            "item": [
              {
                "linkId": "household-head",
                "item": [
                  {
                    "linkId": "head-given-name",
                    "answer": [{ "valueString": "Jane" }]
                  },
                  {
                    "linkId": "head-family-name",
                    "answer": [{ "valueString": "Doe" }]
                  },
                  {
                    "linkId": "head-gender",
                    "answer": [
                      {
                        "valueCoding": {
                          "system": "http://hl7.org/fhir/administrative-gender",
                          "code": "female",
                          "display": "Female"
                        }
                      }
                    ]
                  }
                ]
              }
            ]
          },
          {
            "linkId": "page-household-members",
            "item": [
              {
                "linkId": "household-members",
                "item": [
                  {
                    "linkId": "member-given-name",
                    "answer": [{ "valueString": "Junior" }]
                  },
                  {
                    "linkId": "member-family-name",
                    "answer": [{ "valueString": "Doe" }]
                  },
                  {
                    "linkId": "member-gender",
                    "answer": [
                      {
                        "valueCoding": {
                          "system": "http://hl7.org/fhir/administrative-gender",
                          "code": "male",
                          "display": "Male"
                        }
                      }
                    ]
                  },
                  {
                    "linkId": "member-relationship-to-head",
                    "answer": [
                      {
                        "valueCoding": {
                          "system": "http://terminology.hl7.org/CodeSystem/v3-RoleCode",
                          "code": "CHILD",
                          "display": "Child"
                        }
                      }
                    ]
                  }
                ]
              },
              {
                "linkId": "household-members",
                "item": [
                  {
                    "linkId": "member-given-name",
                    "answer": [{ "valueString": "John" }]
                  },
                  {
                    "linkId": "member-family-name",
                    "answer": [{ "valueString": "Doe" }]
                  },
                  {
                    "linkId": "member-gender",
                    "answer": [
                      {
                        "valueCoding": {
                          "system": "http://hl7.org/fhir/administrative-gender",
                          "code": "male",
                          "display": "Male"
                        }
                      }
                    ]
                  },
                  {
                    "linkId": "member-relationship-to-head",
                    "answer": [
                      {
                        "valueCoding": {
                          "system": "http://terminology.hl7.org/CodeSystem/v3-RoleCode",
                          "code": "SPS",
                          "display": "Spouse"
                        }
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

  private fun householdMembersResponseJson(): String =
    """
      {
        "resourceType": "QuestionnaireResponse",
        "questionnaire": "https://example.org/fhir/Questionnaire/household-members-template-extract",
        "status": "completed",
        "item": [
          {
            "linkId": "page-household-members",
            "item": [
              {
                "linkId": "household-members",
                "item": [
                  {
                    "linkId": "member-given-name",
                    "answer": [{ "valueString": "Ivy" }]
                  },
                  {
                    "linkId": "member-family-name",
                    "answer": [{ "valueString": "Banda" }]
                  },
                  {
                    "linkId": "member-gender",
                    "answer": [
                      {
                        "valueCoding": {
                          "system": "http://hl7.org/fhir/administrative-gender",
                          "code": "female",
                          "display": "Female"
                        }
                      }
                    ]
                  },
                  {
                    "linkId": "member-relationship-to-head",
                    "answer": [
                      {
                        "valueCoding": {
                          "system": "http://terminology.hl7.org/CodeSystem/v3-RoleCode",
                          "code": "CHILD",
                          "display": "Child"
                        }
                      }
                    ]
                  }
                ]
              },
              {
                "linkId": "household-members",
                "item": [
                  {
                    "linkId": "member-given-name",
                    "answer": [{ "valueString": "Noah" }]
                  },
                  {
                    "linkId": "member-family-name",
                    "answer": [{ "valueString": "Banda" }]
                  },
                  {
                    "linkId": "member-gender",
                    "answer": [
                      {
                        "valueCoding": {
                          "system": "http://hl7.org/fhir/administrative-gender",
                          "code": "male",
                          "display": "Male"
                        }
                      }
                    ]
                  },
                  {
                    "linkId": "member-relationship-to-head",
                    "answer": [
                      {
                        "valueCoding": {
                          "system": "http://terminology.hl7.org/CodeSystem/v3-RoleCode",
                          "code": "EXT",
                          "display": "Other relative"
                        }
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
