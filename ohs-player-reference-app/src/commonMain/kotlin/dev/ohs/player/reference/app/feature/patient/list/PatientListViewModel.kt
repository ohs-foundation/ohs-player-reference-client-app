package dev.ohs.player.reference.app.feature.patient.list

import androidx.lifecycle.ViewModel
import dev.ohs.player.reference.app.data.model.PatientView
import dev.ohs.player.reference.app.data.repository.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PatientListViewModel : ViewModel() {

    private val _patients = MutableStateFlow<List<PatientView>>(emptyList())
    val patients: StateFlow<List<PatientView>> = _patients.asStateFlow()

    init {
        _patients.value = PatientRepository.getAll()
    }
}
