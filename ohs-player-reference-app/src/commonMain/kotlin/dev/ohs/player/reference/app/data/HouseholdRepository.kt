package dev.ohs.player.reference.app.data

import dev.ohs.fhir.model.r4.FhirR4Json
import dev.ohs.fhir.model.r4.Group
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.player.library.model.SearchResult
import kotlinx.serialization.decodeFromString

class HouseholdRepository {
    private val fhirJson = FhirR4Json { ignoreUnknownKeys = true }
  private val simulatedDb: Map<String, SearchResult<Group>> by lazy {
    // Realistic household data: each entry contains Group info and optional list of Patients
    val households = listOf(
      HouseholdData(
        id = "G-1", name = "Smith", quantity = 2,
        members = listOf(
          PatientData("P-1-1", "John", "Smith", "1980-05-15", "male", listOf("Amoxicillin 500mg", "Ibuprofen 200mg")),
          PatientData("P-1-2", "Jane", "Smith", "1982-11-23", "female", listOf("Lisinopril 10mg"))
        )
      ),
      HouseholdData(
        id = "G-2", name = "Johnson", quantity = 0, members = emptyList()
      ),
      HouseholdData(
        id = "G-3", name = "Williams", quantity = 2,
        members = listOf(
          PatientData("P-3-1", "Robert", "Williams", "1975-03-10", "male"),
          PatientData("P-3-2", "Maria", "Williams", "1978-07-19", "female")
        )
      ),
      HouseholdData(
        id = "G-4", name = "Brown", quantity = 0, members = emptyList()
      ),
      HouseholdData(
        id = "G-5", name = "Jones", quantity = 3,
        members = listOf(
          PatientData("P-5-1", "James", "Jones", "1990-12-01", "male"),
          PatientData("P-5-2", "Mary", "Jones", "1992-04-25", "female"),
          PatientData("P-5-3", "David", "Jones", "2015-08-30", "male")
        )
      ),
      HouseholdData(
        id = "G-6", name = "Garcia", quantity = 0, members = emptyList()
      ),
      HouseholdData(
        id = "G-7", name = "Miller", quantity = 2,
        members = listOf(
          PatientData("P-7-1", "Patricia", "Miller", "1985-09-12", "female"),
          PatientData("P-7-2", "Linda", "Miller", "1988-02-28", "female")
        )
      ),
      HouseholdData(
        id = "G-8", name = "Davis", quantity = 0, members = emptyList()
      ),
      HouseholdData(
        id = "G-9", name = "Rodriguez", quantity = 2,
        members = listOf(
          PatientData("P-9-1", "Michael", "Rodriguez", "1972-06-17", "male"),
          PatientData("P-9-2", "Elizabeth", "Rodriguez", "1975-10-02", "female")
        )
      ),
      HouseholdData(
        id = "G-10", name = "Martinez", quantity = 1,
        members = listOf(
          PatientData("P-10-1", "Christopher", "Martinez", "2000-07-04", "male")
        )
      )
    )

    households.associate { h ->
      // Build Group JSON using actual human data
      val groupJson = """
        {
          "resourceType": "Group",
          "id": "${h.id}",
          "name": "${h.name} Household",
          "quantity": ${h.quantity},
          "actual": true,
          "active": true,
          "type": "person"
        }
        """.trimIndent()

      val group = fhirJson.decodeFromString(groupJson) as Group

      val included = if (h.members.isNotEmpty()) {
        val patients = h.members.map { member ->
          val patientJson = """
                {
                  "resourceType": "Patient",
                  "id": "${member.id}",
                  "name": [{"family": "${member.family}", "given": ["${member.given}"]}],
                  "birthDate": "${member.birthDate}",
                  "gender": "${member.gender}"
                }
                """.trimIndent()
          fhirJson.decodeFromString(patientJson)
        }
        
        val medications = h.members.flatMap { member ->
          member.medications.mapIndexed { index, med ->
            val medicationJson = """
                {
                  "resourceType": "MedicationStatement",
                  "id": "MS-${member.id}-$index",
                  "status": "active",
                  "medicationCodeableConcept": { "text": "$med" },
                  "subject": { "reference": "Patient/${member.id}" }
                }
                """.trimIndent()
            fhirJson.decodeFromString(medicationJson)
          }
        }
        
        mapOf(
          "Patient" to patients,
          "MedicationStatement" to medications
        )
      } else {
        emptyMap()
      }

      h.id to SearchResult(resource = group, included = included)
    }
  }

  // Helper data classes for seeding
  private data class HouseholdData(
    val id: String,
    val name: String,
    val quantity: Int,
    val members: List<PatientData>
  )

  private data class PatientData(
    val id: String,
    val given: String,
    val family: String,
    val birthDate: String,
    val gender: String,
    val medications: List<String> = emptyList()
  )
    fun getAllHouseholds(): List<SearchResult<Group>> = simulatedDb.values.toList()

    fun getHouseholdById(id: String): SearchResult<Group>? = simulatedDb[id]
}
