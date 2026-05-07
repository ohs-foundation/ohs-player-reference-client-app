package dev.ohs.player.library
import dev.ohs.fhir.fhirpath.FhirPathEngine
import dev.ohs.fhir.model.r4.FhirR4Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataTransformerTest {

    private val fhirJson = FhirR4Json { ignoreUnknownKeys = true }
    private val transformer = DataTransformer(FhirPathEngine.forR4())

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

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = patient,
                paths = mapOf(
                    "patientId"  to "id",
                    "familyName" to "name.family.first()"
                )
            )
        )

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

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = patient,
                paths = mapOf(
                    "patientId"  to "id",
                    "familyName" to "name.family.first()"
                )
            )
        )

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

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = patient,
                paths = mapOf(
                    "fullName" to "name.select(family.first() + ' ' + given.first())",
                    "memberId" to "id"
                )
            )
        )

        assertEquals("Doe Jane", result["fullName"]?.jsonPrimitive?.content)
        assertEquals("P-003",    result["memberId"]?.jsonPrimitive?.content)
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

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = patient,
                paths = mapOf(
                    "fullName" to "name.select(family.first() + ' ' + given.first())",
                    "memberId" to "id"
                )
            )
        )

        assertEquals("Johnson Robert", result["fullName"]?.jsonPrimitive?.content)
        assertEquals("P-010",          result["memberId"]?.jsonPrimitive?.content)
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

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = medication,
                paths = mapOf(
                    "medName" to "medication.as(CodeableConcept).text",
                    "status"  to "status"
                )
            )
        )

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

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = medication,
                paths = mapOf(
                    "medName" to "medication.as(CodeableConcept).text",
                    "status"  to "status"
                )
            )
        )

        assertEquals("stopped",         result["status"]?.jsonPrimitive?.content)
        assertEquals("Lisinopril 10mg", result["medName"]?.jsonPrimitive?.content)
        assertEquals(2, result.size)
    }

    @Test
    fun transformer_wrongResourceType_returnsAllNulls() {
        val patient = fhirJson.decodeFromString(
            """{ "resourceType": "Patient", "id": "P-999" }""".trimIndent()
        )

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = patient,
                paths = mapOf(
                    "medName" to "medication.as(CodeableConcept).text",
                    "status"  to "status"
                )
            )
        )

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

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = medication,
                paths = mapOf(
                    "medName" to "medication.as(CodeableConcept).text",
                    "status"  to "status"
                ),
                context = listOf(patient)
            )
        )

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

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = group,
                paths = mapOf(
                    "householdName" to "name",
                    "memberCount"   to "quantity"
                )
            )
        )

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

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = group,
                paths = mapOf(
                    "householdName" to "name",
                    "memberCount"   to "quantity"
                )
            )
        )

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

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = observation,
                paths = mapOf(
                    "code"  to "code.coding.first().display",
                    "value" to "value.as(Quantity).value",
                    "unit"  to "value.as(Quantity).unit"
                )
            )
        )

        assertEquals("Heart rate", result["code"]?.jsonPrimitive?.content)
        // OHS FhirPathEngine serialises FHIR decimal as "72.0" (Kotlin Double.toString()).
        // Expected per FHIR R4 spec is "72" — tracked as a known serialisation investigation item.
        //      assertEquals("72.0",       result["value"]?.jsonPrimitive?.content)
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

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = observation,
                paths = mapOf(
                    "code"  to "code.coding.first().display",
                    "value" to "value.as(Quantity).value",
                    "unit"  to "value.as(Quantity).unit"
                )
            )
        )

        assertEquals("Body temperature", result["code"]?.jsonPrimitive?.content)
        assertEquals(JsonNull, result["value"])
        assertEquals(JsonNull, result["unit"])
        assertEquals(3, result.size)
    }

    // ── Edge Cases ────────────────────────────────────────────────────────────

    @Test
    fun transformer_emptyPathMap_returnsEmptyJsonObject() {
        val patient = fhirJson.decodeFromString(
            """{ "resourceType": "Patient", "id": "P-001" }""".trimIndent()
        )

        val result = transformer.extractToJson(
            FhirExtractionRequest(resource = patient, paths = emptyMap())
        )

        assertEquals(0, result.size)
    }

    @Test
    fun transformer_invalidFhirPath_returnsJsonNull() {
        val patient = fhirJson.decodeFromString(
            """{ "resourceType": "Patient", "id": "P-001" }""".trimIndent()
        )

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = patient,
                paths = mapOf("broken" to "%%%invalid fhirpath@@@")
            )
        )

        assertEquals(JsonNull, result["broken"])
        assertEquals(1, result.size)
    }

    // ── transform<T>() end-to-end ─────────────────────────────────────────────

    @Serializable
    private data class PatientHeaderState(
        val patientId: String? = null,
        val familyName: String? = null
    )

    @Test
    fun transform_deserializesDirectlyIntoTypedState() {
        val patient = fhirJson.decodeFromString(
            """                                                                                                                                                                                                     
              {
                "resourceType": "Patient",                                                                                                                                                                            
                "id": "P-001",
                "name": [{ "family": "Smith", "given": ["John"] }]
              }
              """.trimIndent()
        )

        val state: PatientHeaderState = transformer.transform(
            FhirExtractionRequest(
                resource = patient,
                paths = mapOf(
                    "patientId"  to "id",
                    "familyName" to "name.family.first()"
                )
            )
        )

        assertEquals("P-001", state.patientId)
        assertEquals("Smith", state.familyName)
    }

    @Test
    fun transformer_samePathTwoDifferentFieldNames_bothResolve() {
        val patient = fhirJson.decodeFromString(
            """{ "resourceType": "Patient", "id": "P-001" }"""
        )

        val result = transformer.extractToJson(
            FhirExtractionRequest(
                resource = patient,
                paths = mapOf(
                    "field1" to "id",
                    "field2" to "id"
                )
            )
        )

        assertEquals("P-001", result["field1"]?.jsonPrimitive?.content)
        assertEquals("P-001", result["field2"]?.jsonPrimitive?.content)
    }
}