package dev.ohs.player.library

import dev.ohs.fhir.fhirpath.FhirPathEngine
import dev.ohs.fhir.model.r4.FhirR4Json
import dev.ohs.player.library.domain.PatientState
import dev.ohs.player.library.domain.model.SelectBlock
import dev.ohs.player.library.domain.model.ViewColumn
import dev.ohs.player.library.domain.model.ViewDefinition
import dev.ohs.player.library.domain.transform.DataTransformer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataTransformerTest {
    private val fhirJson = FhirR4Json { ignoreUnknownKeys = true }
    private val transformer = DataTransformer(FhirPathEngine.forR4())

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
                { "system": "phone", "value":"+254700000001", "use": "mobile" },
                { "system": "email", "value":"john.smith@example.com" }
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
            """.trimIndent(),
        )
    }

    @Test
    fun transform_withFullViewDefinition_populatesAllStateFields() = runTest {
        val state =
            transformer.transform<PatientState>(
                patient,
                ViewDefinition(
                    name = "patient_header",
                    resource = "Patient",
                    select =
                        listOf(
                            SelectBlock(
                                column =
                                    listOf(
                                        ViewColumn(name = "patientId", path = "id"),
                                        ViewColumn(name = "familyName", path = "name.family.first()"),
                                        ViewColumn(name = "fullName", path = "name.select(family.first() + ' ' + given.first())"),
                                        ViewColumn(name = "gender", path = "gender"),
                                        ViewColumn(name = "birthDate", path = "birthDate"),
                                        ViewColumn(name = "phone", path = "telecom.where(system = 'phone').value.first()"),
                                        ViewColumn(name = "email", path = "telecom.where(system = 'email').value.first()"),
                                        ViewColumn(name = "city", path = "address.city.first()"),
                                        ViewColumn(name = "country", path = "address.country.first()"),
                                    ),
                            ),
                        ),
                ),
            )
        assertEquals("P-001", state.patientId)
        assertEquals("Smith", state.familyName)
        assertEquals("Smith John", state.fullName)
        assertEquals("male", state.gender)
        assertEquals("1980-05-15", state.birthDate)
        assertEquals("+254700000001", state.phone)
        assertEquals("john.smith@example.com", state.email)
        assertEquals("Nairobi", state.city)
        assertEquals("KE", state.country)
    }

    @Test
    fun transform_withMissingResourceField_returnsNullForThatField() = runTest  {
        val state =
            transformer.transform<PatientState>(
                patient,
                ViewDefinition(
                    name = "missing_field",
                    resource = "Patient",
                    select =
                        listOf(
                            SelectBlock(
                                column =
                                    listOf(
                                        ViewColumn(name = "patientId", path = "id"),
                                        ViewColumn(name = "fullName", path = "deceased"),
                                    ),
                            ),
                        ),
                ),
            )
        assertEquals("P-001", state.patientId)
        assertNull(state.fullName)
    }

    @Test
    fun transform_withInvalidFhirPath_returnsNullForThatField() = runTest  {
        val state =
            transformer.transform<PatientState>(
                patient,
                ViewDefinition(
                    name = "broken_path",
                    resource = "Patient",
                    select =
                        listOf(
                            SelectBlock(
                                column =
                                    listOf(
                                        ViewColumn(name = "patientId", path = "id"),
                                        ViewColumn(name = "fullName", path = "%%%invalid@@@"),
                                    ),
                            ),
                        ),
                ),
            )
        assertEquals("P-001", state.patientId)
        assertNull(state.fullName)
    }

    @Test
    fun transform_withEmptySelect_returnsAllNullState() = runTest  {
        val state =
            transformer.transform<PatientState>(
                patient,
                ViewDefinition(name = "empty", resource = "Patient"),
            )
        assertNull(state.patientId)
        assertNull(state.familyName)
        assertNull(state.gender)
    }

    @Test
    fun transform_withNullColumnNameOrPath_skipsColumn() = runTest  {
        val state =
            transformer.transform<PatientState>(
                patient,
                ViewDefinition(
                    name = "null_columns",
                    resource = "Patient",
                    select =
                        listOf(
                            SelectBlock(
                                column =
                                    listOf(
                                        ViewColumn(name = "patientId", path = "id"),
                                        ViewColumn(name = null, path = "id"),
                                        ViewColumn(name = "familyName", path = null),
                                    ),
                            ),
                        ),
                ),
            )
        assertEquals("P-001", state.patientId)
        assertNull(state.familyName)
    }

    @Test
    fun transform_withWhereFilterNoMatch_returnsNullForThatField() = runTest  {
        val state =
            transformer.transform<PatientState>(
                patient,
                ViewDefinition(
                    name = "no_match",
                    resource = "Patient",
                    select =
                        listOf(
                            SelectBlock(
                                column =
                                    listOf(
                                        ViewColumn(name = "patientId", path = "id"),
                                        ViewColumn(name = "phone", path = "telecom.where(system = 'fax').value.first()"),
                                    ),
                            ),
                        ),
                ),
            )
        assertEquals("P-001", state.patientId)
        assertNull(state.phone)
    }
}
