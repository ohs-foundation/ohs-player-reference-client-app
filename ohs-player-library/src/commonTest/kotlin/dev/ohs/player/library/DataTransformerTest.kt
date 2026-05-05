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

    private val vitalsSignsViewDef = ViewDefinition(
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


}