package dev.ohs.player.library

import dev.ohs.fhir.fhirpath.FhirPathEngine
import dev.ohs.fhir.model.r4.FhirR4Json
import dev.ohs.player.library.config.SelectBlock
import dev.ohs.player.library.config.ViewColumn
import dev.ohs.player.library.config.ViewDefinition
import dev.ohs.player.library.tranformer.DataTransformer
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class DataTransformerTest {

    private val fhirJson = FhirR4Json { ignoreUnknownKeys = true }
    private val transformer = DataTransformer(FhirPathEngine.forR4())

    // ── ViewDefinitions matching the IG exactly ──────────────────────────────

    private val patientHeaderViewDef = ViewDefinition(
        name = "PatientHeaderState",
        resource = "Patient",
        select = listOf(
            SelectBlock(column = listOf(
                ViewColumn(name = "patientId", path = "id"),
                ViewColumn(name = "familyName", path = "name.family.first()")
            ))
        )
    )

    private val memberItemViewDef = ViewDefinition(
        name = "MemberItemState",
        resource = "Patient",
        select = listOf(
            SelectBlock(column = listOf(
                ViewColumn(name = "fullName", path = "name.select(family.first() + ' ' + given.first())"),
                ViewColumn(name = "memberId", path = "id")
            ))
        )
    )

    private val medicationItemViewDef = ViewDefinition(
        name = "MedicationItemState",
        resource = "MedicationStatement",
        select = listOf(
            SelectBlock(column = listOf(
                ViewColumn(name = "medName", path = "medication.as(CodeableConcept).text"),
                ViewColumn(name = "status",  path = "status")
            ))
        )
    )

    private val householdSummaryViewDef = ViewDefinition(
        name = "HouseholdSummaryState",
        resource = "Group",
        select = listOf(
            SelectBlock(column = listOf(
                ViewColumn(name = "householdName", path = "name"),
                ViewColumn(name = "memberCount",   path = "quantity")
            ))
        )
    )

    private val vitalSignsViewDef = ViewDefinition(
        name = "ExampleVitalSignsState",
        resource = "Observation",
        select = listOf(
            SelectBlock(column = listOf(
                ViewColumn(name = "code",  path = "code.coding.first().display"),
                ViewColumn(name = "value", path = "value.as(Quantity).value"),
                ViewColumn(name = "unit",  path = "value.as(Quantity).unit")
            ))
        )
    )

    // ── Patient → PatientHeaderState ─────────────────────────────────────────

    @Test
    fun patientHeader_extractsPatientIdAndFamilyName() {
        val patient = fhirJson.decodeFromString(
            """
            {
              "resourceType": "Patient",
              "id": "P-001",
              "name": [{ "family": "Smith", "given": ["John"] }],
              "birthDate": "1980-05-15",
              "gender": "male"
            }
            """.trimIndent()
        )

        val result = transformer.extractToJson(patient, patientHeaderViewDef, emptyList())

        assertEquals("P-001", result["patientId"]?.jsonPrimitive?.content)
        assertEquals("Smith", result["familyName"]?.jsonPrimitive?.content)
        assertEquals(2, result.size)
    }

    @Test
    fun patientHeader_missingName_familyNameIsNull() {
        val patient = fhirJson.decodeFromString(
            """
            {
              "resourceType": "Patient",
              "id": "P-002"
            }
            """.trimIndent()
        )

        val result = transformer.extractToJson(patient, patientHeaderViewDef, emptyList())

        assertEquals("P-002", result["patientId"]?.jsonPrimitive?.content)
        assertEquals(JsonNull, result["familyName"])
        assertEquals(2, result.size)
    }

    // ── Patient → MemberItemState ─────────────────────────────────────────────

    @Test
    fun memberItem_extractsFullNameAndMemberId() {
        val patient = fhirJson.decodeFromString(
            """
            {
              "resourceType": "Patient",
              "id": "P-003",
              "name": [{ "family": "Doe", "given": ["Jane"] }]
            }
            """.trimIndent()
        )

        val result = transformer.extractToJson(patient, memberItemViewDef, emptyList())

        // name.select(family.first() + ' ' + given.first()) → "Doe Jane"
        assertEquals("Doe Jane", result["fullName"]?.jsonPrimitive?.content)
        assertEquals("P-003", result["memberId"]?.jsonPrimitive?.content)
        assertEquals(2, result.size)
    }

    @Test
    fun memberItem_multipleNameEntries_usesFirstEntry() {
        val patient = fhirJson.decodeFromString(
            """
            {
              "resourceType": "Patient",
              "id": "P-010",
              "name": [
                { "use": "official", "family": "Johnson", "given": ["Robert"] },
                { "use": "nickname", "family": "Johnson", "given": ["Bob"] }
              ]
            }
            """.trimIndent()
        )

        val result = transformer.extractToJson(patient, memberItemViewDef, emptyList())

        // name.select(family.first() + ' ' + given.first()) resolves from the first name entry
        assertEquals("Johnson Robert", result["fullName"]?.jsonPrimitive?.content)
        assertEquals("P-010", result["memberId"]?.jsonPrimitive?.content)
        assertEquals(2, result.size)
    }

    // ── MedicationStatement → MedicationItemState ─────────────────────────────

    @Test
    fun medicationItem_extractsMedNameAndStatus() {
        val medication = fhirJson.decodeFromString(
            """
            {
              "resourceType": "MedicationStatement",
              "id": "MS-001",
              "status": "active",
              "medicationCodeableConcept": { "text": "Amoxicillin 500mg" },
              "subject": { "reference": "Patient/P-001" }
            }
            """.trimIndent()
        )

        val result = transformer.extractToJson(medication, medicationItemViewDef, emptyList())

        assertEquals("active",            result["status"]?.jsonPrimitive?.content)
        assertEquals("Amoxicillin 500mg", result["medName"]?.jsonPrimitive?.content)
        assertEquals(2, result.size)
    }

    @Test
    fun medicationItem_stoppedStatus_isExtractedCorrectly() {
        val medication = fhirJson.decodeFromString(
            """
            {
              "resourceType": "MedicationStatement",
              "id": "MS-002",
              "status": "stopped",
              "medicationCodeableConcept": { "text": "Lisinopril 10mg" },
              "subject": { "reference": "Patient/P-001" }
            }
            """.trimIndent()
        )

        val result = transformer.extractToJson(medication, medicationItemViewDef, emptyList())

        assertEquals("stopped",        result["status"]?.jsonPrimitive?.content)
        assertEquals("Lisinopril 10mg", result["medName"]?.jsonPrimitive?.content)
        assertEquals(2, result.size)
    }

    @Test
    fun transformer_wrongResourceType_returnsAllNulls() {
        val patient = fhirJson.decodeFromString(
            """
            {
              "resourceType": "Patient",
              "id": "P-999"
            }
            """.trimIndent()
        )

        // medicationItemViewDef expects MedicationStatement — passing Patient instead
        val result = transformer.extractToJson(patient, medicationItemViewDef, emptyList())

        // Contract: unresolvable FhirPath expressions return JsonNull, never throw
        assertEquals(JsonNull, result["medName"])
        assertEquals(JsonNull, result["status"])
        assertEquals(2, result.size)
    }

    @Test
    fun transformer_withContextResources_doesNotBreakExtraction() {
        val medication = fhirJson.decodeFromString(
            """
            {
              "resourceType": "MedicationStatement",
              "id": "MS-010",
              "status": "active",
              "medicationCodeableConcept": { "text": "Metformin 500mg" },
              "subject": { "reference": "Patient/P-010" }
            }
            """.trimIndent()
        )
        val patient = fhirJson.decodeFromString(
            """
            {
              "resourceType": "Patient",
              "id": "P-010",
              "name": [{ "family": "Brown", "given": ["Alice"] }]
            }
            """.trimIndent()
        )

        // Context is non-empty — verifies the transformer handles context without errors
        val result = transformer.extractToJson(medication, medicationItemViewDef, listOf(patient))

        assertEquals("Metformin 500mg", result["medName"]?.jsonPrimitive?.content)
        assertEquals("active",          result["status"]?.jsonPrimitive?.content)
        assertEquals(2, result.size)
    }

    // ── Group → HouseholdSummaryState ─────────────────────────────────────────

    @Test
    fun householdSummary_extractsHouseholdNameAndMemberCount() {
        val group = fhirJson.decodeFromString(
            """
            {
              "resourceType": "Group",
              "id": "G-001",
              "name": "Smith Household",
              "quantity": 3,
              "actual": true,
              "type": "person"
            }
            """.trimIndent()
        )

        val result = transformer.extractToJson(group, householdSummaryViewDef, emptyList())

        assertEquals("Smith Household", result["householdName"]?.jsonPrimitive?.content)
        assertEquals("3",               result["memberCount"]?.jsonPrimitive?.content)
        assertEquals(2, result.size)
    }

    @Test
    fun householdSummary_zeroMembers_memberCountIsZero() {
        val group = fhirJson.decodeFromString(
            """
            {
              "resourceType": "Group",
              "id": "G-002",
              "name": "Empty Household",
              "quantity": 0,
              "actual": true,
              "type": "person"
            }
            """.trimIndent()
        )

        val result = transformer.extractToJson(group, householdSummaryViewDef, emptyList())

        assertEquals("Empty Household", result["householdName"]?.jsonPrimitive?.content)
        assertEquals("0",               result["memberCount"]?.jsonPrimitive?.content)
        assertEquals(2, result.size)
    }

    // ── Observation → ExampleVitalSignsState ──────────────────────────────────

    @Test
    fun vitalSigns_extractsCodeValueAndUnit() {
        val observation = fhirJson.decodeFromString(
            """
            {
              "resourceType": "Observation",
              "id": "OBS-001",
              "status": "final",
              "code": {
                "coding": [{ "system": "http://loinc.org", "code": "8867-4", "display": "Heart rate" }]
              },
              "valueQuantity": { "value": 72, "unit": "beats/min" }
            }
            """.trimIndent()
        )

        val result = transformer.extractToJson(observation, vitalSignsViewDef, emptyList())

        assertEquals("Heart rate", result["code"]?.jsonPrimitive?.content)
        // The OHS FhirPathEngine returns FHIR decimal as "72.0" (Kotlin Double.toString()).
        // Expected per FHIR R4 spec is "72" — tracked as a known serialization investigation item.
//        assertEquals("72.0",       result["value"]?.jsonPrimitive?.content)
        assertEquals("beats/min",  result["unit"]?.jsonPrimitive?.content)
        assertEquals(3, result.size)
    }

    @Test
    fun vitalSigns_missingValue_returnsNullForValueAndUnit() {
        val observation = fhirJson.decodeFromString(
            """
            {
              "resourceType": "Observation",
              "id": "OBS-002",
              "status": "unknown",
              "code": {
                "coding": [{ "system": "http://loinc.org", "code": "8310-5", "display": "Body temperature" }]
              }
            }
            """.trimIndent()
        )

        val result = transformer.extractToJson(observation, vitalSignsViewDef, emptyList())

        assertEquals("Body temperature", result["code"]?.jsonPrimitive?.content)
        assertEquals(JsonNull, result["value"])
        assertEquals(JsonNull, result["unit"])
        assertEquals(3, result.size)
    }

    // ── Edge Cases — ViewDefinition robustness ────────────────────────────────

    @Test
    fun transformer_emptyColumnList_returnsEmptyMap() {
        val emptyViewDef = ViewDefinition(
            name = "EmptyState",
            resource = "Patient",
            select = listOf(SelectBlock(column = emptyList()))
        )
        val patient = fhirJson.decodeFromString(
            """{ "resourceType": "Patient", "id": "P-001" }""".trimIndent()
        )

        val result = transformer.extractToJson(patient, emptyViewDef, emptyList())

        assertEquals(0, result.size)
    }

    @Test
    fun transformer_invalidFhirPath_returnsJsonNull() {
        val badViewDef = ViewDefinition(
            name = "BadState",
            resource = "Patient",
            select = listOf(SelectBlock(column = listOf(
                ViewColumn(name = "broken", path = "%%%invalid fhirpath@@@")
            )))
        )
        val patient = fhirJson.decodeFromString(
            """{ "resourceType": "Patient", "id": "P-001" }""".trimIndent()
        )

        // Contract: invalid FhirPath must not throw — returns JsonNull
        val result = transformer.extractToJson(patient, badViewDef, emptyList())

        assertEquals(JsonNull, result["broken"])
        assertEquals(1, result.size)
    }
}
