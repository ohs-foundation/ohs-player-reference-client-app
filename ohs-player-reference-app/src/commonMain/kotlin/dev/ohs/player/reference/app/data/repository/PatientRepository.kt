package dev.ohs.player.reference.app.data.repository

import dev.ohs.player.reference.app.data.datasource.PATIENTS_JSON
import dev.ohs.player.reference.app.data.model.PatientView
import kotlinx.serialization.json.Json

object PatientRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val patients: List<PatientView> by lazy {
        json.decodeFromString<List<PatientView>>(PATIENTS_JSON)
    }

    fun getAll(): List<PatientView> = patients

    fun getById(id: String): PatientView? = patients.find { it.id == id }
}
