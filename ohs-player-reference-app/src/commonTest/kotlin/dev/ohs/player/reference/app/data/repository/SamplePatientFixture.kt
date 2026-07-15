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

import dev.ohs.fhir.model.r4.Resource
import dev.ohs.player.reference.app.util.FhirJson

/** Patient "p1" (Amina Diallo) and related clinical resources, for UI tests. */
object SamplePatientFixture {
  val resources: List<Resource> =
    listOf(
        """
          {
            "resourceType": "Patient",
            "id": "p1",
            "name": [{"family": "Diallo", "given": ["Amina"]}],
            "gender": "female",
            "birthDate": "1990-03-14",
            "active": true,
            "telecom": [
              {"system": "phone", "value": "+221 77 123 4567"},
              {"system": "email", "value": "amina.diallo@example.com"}
            ]
          }
        """,
        """
          {
            "resourceType": "AllergyIntolerance",
            "id": "ai-p1-a",
            "patient": {"reference": "Patient/p1"},
            "code": {"coding": [{"display": "Penicillin"}]},
            "criticality": "high",
            "clinicalStatus": {
              "coding": [
                {
                  "system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical",
                  "code": "active"
                }
              ]
            },
            "reaction": [
              {
                "manifestation": [
                  {
                    "coding": [
                      {"system": "http://snomed.info/sct", "code": "247472004", "display": "Urticaria"}
                    ]
                  }
                ],
                "severity": "moderate"
              }
            ]
          }
        """,
        """
          {
            "resourceType": "MedicationRequest",
            "id": "med-p1-a",
            "status": "active",
            "intent": "order",
            "subject": {"reference": "Patient/p1"},
            "medicationCodeableConcept": {"coding": [{"display": "Salbutamol 100mcg inhaler"}]},
            "dosageInstruction": [{"text": "100mcg PRN"}]
          }
        """,
        """
          {
            "resourceType": "Condition",
            "id": "cond-p1-a",
            "subject": {"reference": "Patient/p1"},
            "code": {"coding": [{"display": "Asthma"}]},
            "clinicalStatus": {
              "coding": [
                {"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}
              ]
            },
            "onsetDateTime": "2015-06-01"
          }
        """,
        """
          {
            "resourceType": "Immunization",
            "id": "imm-p1-a",
            "status": "completed",
            "vaccineCode": {"coding": [{"display": "BCG vaccine"}]},
            "patient": {"reference": "Patient/p1"},
            "occurrenceDateTime": "1990-03-14"
          }
        """,
      )
      .map { FhirJson.instance.decodeFromString(Resource.serializer(), it.trimIndent()) }
}
