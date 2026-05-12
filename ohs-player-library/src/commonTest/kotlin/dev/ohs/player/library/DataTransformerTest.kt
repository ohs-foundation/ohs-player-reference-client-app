package dev.ohs.player.library

import dev.ohs.fhir.fhirpath.FhirPathEngine
import dev.ohs.fhir.model.r4.FhirR4Json
import dev.ohs.player.library.model.SelectBlock
import dev.ohs.player.library.model.ViewColumn
import dev.ohs.player.library.model.ViewDefinition
import dev.ohs.player.library.transform.DataTransformer
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataTransformerTest {

    private val fhirJson = FhirR4Json { ignoreUnknownKeys = true }
    private val transformer = DataTransformer(FhirPathEngine.forR4())

    // ── Shared resource ───────────────────────────────────────────────────────
    // Extend this JSON to support new ViewDefinition expressions in future tests.

    private val patient by lazy {
        fhirJson.decodeFromString(
            """                                                                                                                                                                                                     
              {
                "resourceType": "Patient",                                                                                                                                                                            
                "id": "P-001",
                "active": true,
                "name": [
                  {
                    "use": "official",
                    "family": "Smith",
                    "given": ["John", "Michael"]                                                                                                                                                                      
                  }
                ],                                                                                                                                                                                                    
                "gender": "male",
                "birthDate": "1980-05-15",
                "telecom": [
                  { "system": "phone", "value": "+254700000001", "use": "mobile" },
                  { "system": "email", "value": "john.smith@example.com" }                                                                                                                                            
                ],
                "address": [                                                                                                                                                                                          
                  {
                    "use": "home",                                                                                                                                                                                    
                    "line": ["123 Main Street"],
                    "city": "Nairobi",                                                                                                                                                                                
                    "country": "KE"
                  }
                ]                                                                                                                                                                                                     
              }
              """.trimIndent()
        )
    }

    // ── Shared state class ────────────────────────────────────────────────────
    // All fields are nullable with defaults. Each ViewDefinition populates only
    // the fields it declares — unmapped fields remain null.
    // Add new fields here when extending the patient JSON above.

    @Serializable
    private data class PatientState(
        val patientId: String? = null,
        val familyName: String? = null,
        val fullName: String? = null,
        val gender: String? = null,
        val birthDate: String? = null,
        val phone: String? = null,
        val email: String? = null,
        val city: String? = null,
        val country: String? = null
    )

    // ── Single field extractions ──────────────────────────────────────────────

    @Test
    fun extractPatientId() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "patient_id",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "patientId", path = "id")
                )))
            )
        )
        assertEquals("P-001", state.patientId)
        assertNull(state.familyName)
    }

    @Test
    fun extractFamilyName() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "family_name",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "familyName", path = "name.family.first()")
                )))
            )
        )
        assertEquals("Smith", state.familyName)
        assertNull(state.patientId)
    }

    @Test
    fun extractFullNameConcatenated() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "full_name",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "fullName", path = "name.select(family.first() + ' ' + given.first())")
                )))
            )
        )
        assertEquals("Smith John", state.fullName)
    }

    @Test
    fun extractGender() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "gender",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "gender", path = "gender")
                )))
            )
        )
        assertEquals("male", state.gender)
    }

    @Test
    fun extractBirthDate() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "birth_date",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "birthDate", path = "birthDate")
                )))
            )
        )
        assertEquals("1980-05-15", state.birthDate)
    }

    @Test
    fun extractMobilePhone() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "phone",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "phone", path = "telecom.where(system = 'phone').value.first()")
                )))
            )
        )
        assertEquals("+254700000001", state.phone)
    }

    @Test
    fun extractEmail() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "email",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "email", path = "telecom.where(system = 'email').value.first()")
                )))
            )
        )
        assertEquals("john.smith@example.com", state.email)
    }

    @Test
    fun extractAddressFields() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "address",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "city",    path = "address.city.first()"),
                    ViewColumn(name = "country", path = "address.country.first()")
                )))
            )
        )
        assertEquals("Nairobi", state.city)
        assertEquals("KE",      state.country)
    }

    // ── Multiple fields in one ViewDefinition ─────────────────────────────────

    @Test
    fun extractPatientHeaderFields() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "patient_header",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "patientId",  path = "id"),
                    ViewColumn(name = "familyName", path = "name.family.first()"),
                    ViewColumn(name = "gender",     path = "gender"),
                    ViewColumn(name = "birthDate",  path = "birthDate")
                )))
            )
        )
        assertEquals("P-001",      state.patientId)
        assertEquals("Smith",      state.familyName)
        assertEquals("male",       state.gender)
        assertEquals("1980-05-15", state.birthDate)
        assertNull(state.phone)
        assertNull(state.email)
    }

    @Test
    fun extractFullContactProfile() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "contact_profile",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "patientId", path = "id"),
                    ViewColumn(name = "fullName",  path = "name.select(family.first() + ' ' + given.first())"),
                    ViewColumn(name = "phone",     path = "telecom.where(system = 'phone').value.first()"),
                    ViewColumn(name = "email",     path = "telecom.where(system = 'email').value.first()"),
                    ViewColumn(name = "city",      path = "address.city.first()")
                )))
            )
        )
        assertEquals("P-001",                  state.patientId)
        assertEquals("Smith John",             state.fullName)
        assertEquals("+254700000001",          state.phone)
        assertEquals("john.smith@example.com", state.email)
        assertEquals("Nairobi",                state.city)
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun missingFieldRemainsNull() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "missing_field",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "patientId", path = "id"),
                    ViewColumn(name = "fullName",  path = "deceased")
                )))
            )
        )
        assertEquals("P-001", state.patientId)
        assertNull(state.fullName)
    }

    @Test
    fun invalidFhirPathFieldRemainsNull() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "broken",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "patientId", path = "id"),
                    ViewColumn(name = "fullName",  path = "%%%invalid@@@")
                )))
            )
        )
        assertEquals("P-001", state.patientId)
        assertNull(state.fullName)
    }

    @Test
    fun emptySelectAllFieldsNull() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(name = "empty", resource = "Patient")
        )
        assertNull(state.patientId)
        assertNull(state.familyName)
        assertNull(state.gender)
    }

    @Test
    fun columnsWithNullNameOrPathAreSkipped() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "null_columns",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "patientId",  path = "id"),
                    ViewColumn(name = null,         path = "id"),
                    ViewColumn(name = "familyName", path = null)
                )))
            )
        )
        assertEquals("P-001", state.patientId)
        assertNull(state.familyName)
    }
    @Test
    fun whereFilterWithNoMatchReturnsNull() {
        val state = transformer.transform<PatientState>(
            patient,
            ViewDefinition(
                name = "fax",
                resource = "Patient",
                select = listOf(SelectBlock(column = listOf(
                    ViewColumn(name = "patientId", path = "id"),
                    ViewColumn(name = "phone",     path = "telecom.where(system = 'fax').value.first()")
                )))
            )
        )
        assertEquals("P-001", state.patientId)
        assertNull(state.phone)
    }
}