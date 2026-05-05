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
import kotlin.test.assertNotNull

class DataTransformerTest {

    private val fhirJson = FhirR4Json {ignoreUnknownKeys = true}
    private val transformer = DataTransformer(FhirPathEngine.forR4())

    // ViewDefinitions mathcing the IG

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
                ViewColumn(name = "memberCount", path = "quantity")
            ))
        )
    )

    private val vitalSignsViewDef = ViewDefinition(
        name = "ExampleVitalSignsState",
        resource = "Observation",
        select = listOf(
            SelectBlock(column = listOf(
                ViewColumn(name = "code", path = "code.coding.first.display"),
                ViewColumn(name = "value", path = "value.as(Quantity).value"),
                ViewColumn(name = "unit", path = "value.as(Quantity).unit"),
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

        assertEquals("P-003", result["memberId"]?.jsonPrimitive?.content)
        // name.select(family.first() + ' ' + given.first()) → "Doe Jane"
        val fullName = result["fullName"]?.jsonPrimitive?.content
        assertNotNull(fullName)
        assertEquals("Doe Jane", fullName)
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

        assertEquals("active",           result["status"]?.jsonPrimitive?.content)
        assertEquals("Amoxicillin 500mg", result["medName"]?.jsonPrimitive?.content)
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

        assertEquals("stopped",       result["status"]?.jsonPrimitive?.content)
        assertEquals("Lisinopril 10mg", result["medName"]?.jsonPrimitive?.content)
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

        assertEquals("Heart rate",  result["code"]?.jsonPrimitive?.content)
        // FHIR decimal type serialises via toString() as scientific notation
        assertEquals("7.2E+1",      result["value"]?.jsonPrimitive?.content)
        assertEquals("beats/min",   result["unit"]?.jsonPrimitive?.content)
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
    }



}